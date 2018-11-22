package com.mackenziehigh.socius.actors;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;
import java.util.function.Predicate;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Log messages that doe not obey a requirement.
 *
 * @param <T> is the type of the messages flowing through.
 */
public final class Asserter<T>
{
    /**
     * This logger will be used to issue the error-messages.
     */
    private final Logger logger;

    /**
     * This is the logging level of the error-messages.
     */
    private final Level level;

    /**
     * This is the format of the error-message to issue.
     */
    private final String format;

    /**
     * This is the condition that the messages must obey.
     */
    private final Predicate<T> condition;

    private final Processor<T> checker;

    private final Processor<T> validOut;

    private final Processor<T> invalidOut;

    private Asserter (final Stage stage,
                      final Logger logger,
                      final Level level,
                      final String format,
                      final Predicate<T> condition)
    {
        this.logger = logger;
        this.level = level;
        this.format = format;
        this.condition = condition;
        this.checker = Processor.newProcessor(stage, this::onMessage);
        this.validOut = Processor.newProcessor(stage);
        this.invalidOut = Processor.newProcessor(stage);
    }

    private void onMessage (final T message)
    {
        if (condition.test(message))
        {
            validOut.dataIn().send(message);
        }
        else
        {
            logger.log(level, String.format(format, message));
            invalidOut.dataIn().send(message);
        }
    }

    /**
     * Send messages that need to be validated to this input.
     *
     * <p>
     * If the messages are valid, then they will be forwarded
     * to the valid-output after the validity check is performed.
     * </p>
     *
     * <p>
     * If the messages are invalid, then they will be forwarded
     * to the invalid-output after the validity check is performed.
     * </p>
     *
     * @return the data-input.
     */
    public Input<T> dataIn ()
    {
        return checker.dataIn();
    }

    /**
     * Valid messages will be forwarded from the data-input to this output.
     *
     * @return the valid-output.
     */
    public Output<T> validOut ()
    {
        return validOut.dataOut();
    }

    /**
     * Invalid messages will be forwarded from the data-input to this output.
     *
     * @return the invalid-output.
     */
    public Output<T> invalidOut ()
    {
        return invalidOut.dataOut();
    }

    /**
     * Create a builder than can build an <code>Asserter</code>.
     *
     * @param <T> is the type of messages that will pass through.
     * @param stage will be used to create private actors.
     * @return the new builder.
     */
    public static <T> Builder<T> newAsserter (final Stage stage)
    {
        return new Builder<>(stage);
    }

    /**
     * Builder.
     *
     * @param <T> is the type of messages that will pass through.
     */
    public static final class Builder<T>
    {
        private final Stage stage;

        private Logger logger = LogManager.getLogger(Asserter.class);

        private Level level = Level.WARN;

        private String format = "Requirement Violated by Message: %s";

        private Predicate<T> predicate = x -> true;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Specify the error-message to issue when invalid messages are detected.
         *
         * <p>
         * The format string will be passed to the <code>String.format()</code> method.
         * The string representation of the message itself will be given as the only argument.
         * </p>
         *
         * @param format is the error-message to issue.
         * @return this.
         */
        public Builder<T> withFormat (final String format)
        {
            this.format = Objects.requireNonNull(format, "format");
            return this;
        }

        /**
         * Specify the logger to use in order to issue error-messages.
         *
         * @param name identifies the logger.
         * @return this.
         */
        public Builder<T> withLogger (final String name)
        {
            this.logger = LogManager.getLogger(name);
            return this;
        }

        /**
         * Specify the logger to use in order to issue error-messages.
         *
         * @param logger is the logger to use.
         * @return this.
         */
        public Builder<T> withLogger (final Logger logger)
        {
            this.logger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        /**
         * Specify the log-level to use when issuing error-messages.
         *
         * @param level is the desired log-level.
         * @return this.
         */
        public Builder<T> withLevel (final Level level)
        {
            this.level = Objects.requireNonNull(level, "level");
            return this;
        }

        /**
         * Specify the requirement that the messages must obey.
         *
         * @param condition is the requirement.
         * @return this.
         */
        public Builder<T> require (final Predicate<T> condition)
        {
            Objects.requireNonNull(condition, "condition");
            predicate = predicate.and(condition);
            return this;
        }

        /**
         * Build.
         *
         * @return the new asserter.
         */
        public Asserter<T> build ()
        {
            return new Asserter<>(stage, logger, level, format, predicate);
        }
    }

}
