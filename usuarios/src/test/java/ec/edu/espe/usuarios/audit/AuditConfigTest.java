package ec.edu.espe.usuarios.audit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AuditConfigTest {

    private final AuditConfig config = new AuditConfig();

    @Test
    void auditExchange_creaTopicExchangeDurable() {
        TopicExchange exchange = config.auditExchange("audit_exchange");
        assertNotNull(exchange);
        assertEquals("audit_exchange", exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    void auditMessageConverter_noEsNull() {
        MessageConverter converter = config.auditMessageConverter();
        assertNotNull(converter);
    }

    @Test
    void rabbitTemplate_usaElConverter() {
        ConnectionFactory cf = mock(ConnectionFactory.class);
        MessageConverter converter = config.auditMessageConverter();
        RabbitTemplate template = config.rabbitTemplate(cf, converter);
        assertNotNull(template);
        assertSame(converter, template.getMessageConverter());
    }
}
