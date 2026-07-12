package ec.edu.espe.tickets.audit;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Declara el exchange de auditoria (mismo nombre/tipo que espera ms-audit) y
 * configura el RabbitTemplate para serializar los eventos como JSON, que es
 * lo que el consumidor de ms-audit (amqplib + class-validator) espera.
 *
 * <p>El ObjectMapper registra Hibernate6Module porque las entidades que se
 * pasan como "datos" a menudo tienen relaciones LAZY (proxies de Hibernate);
 * sin este modulo, Jackson falla al serializarlas y el evento se pierde en
 * silencio (solo queda un WARN en el log de AuditPublisher).
 */
@Configuration
public class AuditConfig {

    @Bean
    public TopicExchange auditExchange(@Value("${audit.exchange:audit_exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public MessageConverter auditMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // FORCE_LAZY_LOADING=false (default): una relacion LAZY no inicializada
        // se serializa como null en vez de forzar su carga o lanzar excepcion.
        mapper.registerModule(new Hibernate6Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter auditMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(auditMessageConverter);
        return template;
    }
}
