package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public final class Asserter<T>
{
    private final Logger logger;

    private final String format;

    private final Predicate<T> condition;

    private final Processor<T> checker;

    private final Processor<T> validOut;

    private final Processor<T> invalidOut;

    private Asserter (final Stage stage,
                      final Logger logger,
                      final String format,
                      final Predicate<T> condition)
    {
        this.logger = logger;
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

            invalidOut.dataIn().send(message);
        }
    }

    public Input<T> dataIn ()
    {
        return checker.dataIn();
    }

    public Output<T> validOut ()
    {
        return validOut.dataOut();
    }

    public Output<T> invalidOut ()
    {
        return invalidOut.dataOut();
    }

    public static <T> Builder<T> newAsserter (final Stage stage)
    {
        return new Builder<>(stage);
    }

    public static final class Builder<T>
    {
        private final Stage stage;

        private Logger logger;

        private String format;

        private Predicate<T> predicate = x -> true;

        private Builder (final Stage stage)
        {
            this.stage = stage;
        }

        public Builder<T> withFormat (final String format)
        {
            this.format = Objects.requireNonNull(format, "format");
            return this;
        }

        public Builder<T> withLogger (final String name)
        {
            this.logger = LogManager.getLogger(name);
            return this;
        }

        public Builder<T> withLogger (final Logger logger)
        {
            this.logger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        public Builder<T> require (final Predicate<T> condition)
        {
            predicate = predicate.and(condition);
            return this;
        }

        public Asserter<T> build ()
        {
            return new Asserter<>(stage, logger, format, predicate);
        }
    }

}
