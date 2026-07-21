package ec.edu.espe.zonas.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

/** Verifica que los @Bean de AuditConfig se construyan con la configuracion esperada. */
class AuditConfigTest {

    private AuditConfig config;

    @BeforeEach
    void setUp() {
        config = new AuditConfig();
    }

    @Test
    void auditExchangeEsTopicDurableNoAutoDelete() {
        TopicExchange exchange = config.auditExchange("mi_exchange");
        assertNotNull(exchange);
        assertEquals("mi_exchange", exchange.getName());
        assertTrue(exchange.isDurable());
        assertEquals(false, exchange.isAutoDelete());
    }

    @Test
    void auditMessageConverterEsJson() {
        MessageConverter converter = config.auditMessageConverter();
        assertNotNull(converter);
        assertTrue(converter instanceof Jackson2JsonMessageConverter);
    }

    @Test
    void rabbitTemplateUsaElConverterConfigurado() {
        ConnectionFactory cf = mock(ConnectionFactory.class);
        MessageConverter converter = config.auditMessageConverter();

        RabbitTemplate template = config.rabbitTemplate(cf, converter);

        assertNotNull(template);
        assertEquals(converter, template.getMessageConverter());
    }
}
