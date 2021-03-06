package com.umantis.poc.config;

import com.umantis.poc.Consumer;
import com.umantis.poc.GenericConsumer;
import com.umantis.poc.model.CommonMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer Configuration with special setup for exponential backoff message retry consumer.
 * Autocommit is disabled in order to leave the commit responsibility to the consumer.
 *
 * @author David Espinosa.
 */
@Configuration
@EnableKafka
@DependsOn("kafkaTopicRandom")
public class KafkaConsumerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${kafka.servers}")
    private String servers;

    @Value("${consumer.group}")
    private String groupId;

    @Value("${generic.consumer.group}")
    private String genericGroupId;

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        return props;
    }

    @Bean
    public ConsumerFactory<String, CommonMessage> factoryConfig() {
        return new DefaultKafkaConsumerFactory<String, CommonMessage>(consumerConfigs(), new StringDeserializer(), new JsonDeserializer(CommonMessage.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CommonMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CommonMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(factoryConfig());
        factory.getContainerProperties().setAckMode(AbstractMessageListenerContainer.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public Consumer consumer() {
        return new Consumer();
    }

    // up here common configuration, below a new one using GenericMessage type
    @Bean
    public Map<String, Object> genericConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, genericGroupId);
        return props;
    }

    @Bean
    public ConsumerFactory<String, String> genericConsumerConsumerFactoryConfig() {
        return new DefaultKafkaConsumerFactory<String, String>(genericConsumerConfigs(), new StringDeserializer(), new StringDeserializer());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> genericKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(genericConsumerConsumerFactoryConfig());
        // using this messageconverter, the object is correctly unmarshaled but keys and headers are not reachable
        //        factory.setMessageConverter(new StringJsonMessageConverter());
        factory.setMessageConverter(new MessagingMessageConverter());
        return factory;
    }

    @Bean
    public GenericConsumer genericConsumer() {
        LOGGER.info("Creating generic consumer");
        return new GenericConsumer();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
