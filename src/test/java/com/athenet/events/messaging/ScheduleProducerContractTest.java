package com.athenet.events.messaging;

import java.net.URLClassLoader;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import com.athenet.events.service.MatchProjectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

/** Compila el productor real de agenda; no conecta RabbitMQ ni bases. */
@EnabledIfSystemProperty(named = "events.contract.competition-source", matches = ".+")
class ScheduleProducerContractTest {
    @TempDir Path classes;
    @Test void publishedAndCancelledSnapshotsAreAcceptedByRealListener() throws Exception {
        var root = Path.of(System.getProperty("events.contract.competition-source"))
                .toAbsolutePath().resolve("src/main/java/com/athenet/competition");
        var args = new ArrayList<>(List.of("javac","--release","25","-encoding","UTF-8","-classpath",
                System.getProperty("java.class.path"),"-processor","lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
                "-d",classes.toString()));
        for (String path : List.of("messaging/ScheduleEventPublisher.java","messaging/OutboxEventWriter.java",
                "repository/OutboxMessageRepository.java","model/OutboxMessage.java","model/Match.java",
                "model/Competition.java","model/Enrollment.java","model/enums/MatchRound.java",
                "model/enums/MatchStatus.java","model/enums/CompetitionType.java","model/enums/CompetitionStatus.java")) {
            var source = root.resolve(path); assertThat(source).isRegularFile(); args.add(source.toString());
        }
        var compiler = new ProcessBuilder(args).inheritIO().start();
        try {
            assertThat(compiler.waitFor(60,TimeUnit.SECONDS)).isTrue();
            assertThat(compiler.exitValue()).isZero();
        } finally { if (compiler.isAlive()) compiler.destroyForcibly(); }
        try (var loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()},getClass().getClassLoader());
             var validators = Validation.buildDefaultValidatorFactory()) {
            Object match = instance(loader,"model.Match");
            set(match,"Id",Long.class,31L); set(match,"CompetitionId",Long.class,4L);
            Class<?> round = loader.loadClass("com.athenet.competition.model.enums.MatchRound");
            set(match,"Round",round,round.getField("QUARTER_FINAL").get(null));
            set(match,"Date",LocalDate.class,LocalDate.of(2026,10,24)); set(match,"Time",LocalTime.class,LocalTime.of(22,0));
            set(match,"EndDate",LocalDate.class,LocalDate.of(2026,10,25)); set(match,"EndTime",LocalTime.class,LocalTime.of(0,30));
            set(match,"HomeTeamId",Long.class,12L); set(match,"AwayTeamId",Long.class,15L);
            set(match,"DelegateOid",String.class,"3f2b9c1e-5a7d-4c1b-9e2f-1d0a8b6c4e21");
            set(match,"DelegateDisplayName",String.class,"Delegado");
            set(match,"ScheduleConfirmed",boolean.class,true); set(match,"ScheduleVersion",int.class,1);
            Object competition = instance(loader,"model.Competition");
            set(competition,"Id",Long.class,4L); set(competition,"Name",String.class,"Copa Instituciones");
            set(competition,"SportId",Long.class,3L); set(competition,"CompetitionImg",String.class,"https://example.test/cup.png");
            Object home = instance(loader,"model.Enrollment"); Object away = instance(loader,"model.Enrollment");
            set(home,"TeamName",String.class,"Halcones"); set(away,"TeamName",String.class,"Pumas");
            Class<?> writerType = loader.loadClass("com.athenet.competition.messaging.OutboxEventWriter");
            Class<?> repoType = loader.loadClass("com.athenet.competition.repository.OutboxMessageRepository");
            Object writer = mock(writerType); Object repository = mock(repoType);
            var mapper = new ObjectMapper();
            Class<?> publisherType = loader.loadClass("com.athenet.competition.messaging.ScheduleEventPublisher");
            Object publisher = publisherType.getConstructor(writerType,repoType,ObjectMapper.class).newInstance(writer,repository,mapper);
            publisherType.getMethod("publish",match.getClass(),competition.getClass(),home.getClass(),away.getClass())
                    .invoke(publisher,match,competition,home,away);
            var calls = new ArrayList<>(mockingDetails(writer).getInvocations());
            Object envelope = calls.getFirst().getArgument(3);
            String published = mapper.writeValueAsString(envelope);
            Object stored = instance(loader,"model.OutboxMessage"); set(stored,"Payload",String.class,published);
            when(repoType.getMethod("findByRoutingKeyAndAggregateIdAndRevision",String.class,Long.class,int.class)
                    .invoke(repository,"partido.actualizado",31L,1)).thenReturn(Optional.of(stored));
            publisherType.getMethod("cancel",match.getClass()).invoke(publisher,match);
            calls = new ArrayList<>(mockingDetails(writer).getInvocations());
            assertThat(calls).hasSize(2);
            var projections = mock(MatchProjectionService.class);
            var listener = new MatchScheduleListener(mapper,validators.getValidator(),projections);
            for (var call : calls) {
                assertThat(call.getArgument(0).toString()).isEqualTo("partido.actualizado");
                listener.receive(new Message(mapper.writeValueAsBytes(call.getArgument(3)),new MessageProperties()));
            }
            var accepted = ArgumentCaptor.forClass(MatchScheduleEnvelope.Data.class);
            verify(projections,times(2)).accept(accepted.capture());
            assertThat(accepted.getAllValues()).extracting(MatchScheduleEnvelope.Data::status).containsExactly("PUBLISHED","CANCELLED");
            assertThat(accepted.getAllValues()).extracting(MatchScheduleEnvelope.Data::scheduleVersion).containsExactly(1,2);
            assertThat(accepted.getValue().endDate()).isEqualTo("2026-10-25");
        }
    }
    private static Object instance(ClassLoader loader,String suffix) throws Exception {
        return loader.loadClass("com.athenet.competition."+suffix).getConstructor().newInstance();
    }
    private static void set(Object target,String property,Class<?> type,Object value) throws Exception {
        target.getClass().getMethod("set"+property,type).invoke(target,value);
    }
}
