package io.github.positionpal.notification.entrypoint

import io.github.positionpal.notification.application.tokens.impl.UsersTokensServiceImpl
import io.github.positionpal.notification.fcm.Firebase
import io.github.positionpal.notification.fcm.FirebaseCloudNotificationPublisher
import io.github.positionpal.notification.grpc.GrpcServer
import io.github.positionpal.notification.grpc.tokens.GrpcUsersTokensService
import io.github.positionpal.notification.mom.RabbitMQ
import io.github.positionpal.notification.mom.RabbitMQGroupsEventsConsumer
import io.github.positionpal.notification.mom.RabbitMQNotificationsConsumer
import io.github.positionpal.notification.storage.Postgres
import io.github.positionpal.notification.storage.groups.PostgresGroupsRepository
import io.github.positionpal.notification.storage.tokens.PostgresUsersTokensRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * The entry point of the service.
 */
object Launcher {

    /**
     * The main function of the application.
     * @param args The command line arguments.
     */
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        val firebaseConfiguration = Firebase.Configuration("service-account.json")
        val firebase = Firebase.create(firebaseConfiguration).getOrThrow()
        val rabbitMqConfiguration = RabbitMQ.Configuration(
            host = "localhost",
            virtualHost = "/",
            port = 5672,
            username = "guest",
            password = "admin",
        )
        val postgresConfiguration = Postgres.Configuration(
            databaseName = "notifications_service",
            username = "postgres",
            password = "postgres",
            host = "localhost",
            port = 5432,
        )
        Postgres(postgresConfiguration).connect().getOrThrow()
        val groupsRepository = PostgresGroupsRepository()
        val usersTokensRepository = PostgresUsersTokensRepository()
        val usersTokensService = GrpcUsersTokensService(UsersTokensServiceImpl(usersTokensRepository))
        val server = GrpcServer(GrpcServer.Configuration(services = listOf(usersTokensService)))
        val grpcService = launch { server.start() }
        val rabbitMqNotificationService = RabbitMQNotificationsConsumer(
            FirebaseCloudNotificationPublisher(firebase, usersTokensRepository, groupsRepository),
            rabbitMqConfiguration,
        )
        val rabbitMqGroupsService = RabbitMQGroupsEventsConsumer(groupsRepository, rabbitMqConfiguration)
        rabbitMqNotificationService.setup()
        rabbitMqGroupsService.setup()
        grpcService.join()
    }
}
