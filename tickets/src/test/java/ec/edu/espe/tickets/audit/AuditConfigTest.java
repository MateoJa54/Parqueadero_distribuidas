package ec.edu.espe.tickets.audit;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

/** Verifica que los @Bean de auditoria se construyen correctamente. */
class AuditConfigTest {

    private final AuditConfig config = new AuditConfig();

    @Test
    void auditExchange_creaTopicExchangeConNombre() {
        TopicExchange exchange = config.auditExchange("mi_exchange");
        assertNotNull(exchange);
        org.junit.jupiter.api.Assertions.assertEquals("mi_exchange", exchange.getName());
        org.junit.jupiter.api.Assertions.assertTrue(exchange.isDurable());
    }

    @Test
    void auditMessageConverter_esJacksonJson() {
        MessageConverter converter = config.auditMessageConverter();
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }

    @Test
    void rabbitTemplate_seConstruyeConConverter() {
        ConnectionFactory cf = mock(ConnectionFactory.class);
        MessageConverter converter = config.auditMessageConverter();

        RabbitTemplate template = config.rabbitTemplate(cf, converter);

        assertNotNull(template);
        org.junit.jupiter.api.Assertions.assertSame(converter, template.getMessageConverter());
    }
}
