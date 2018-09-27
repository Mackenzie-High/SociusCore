package com.mackenziehigh.socius.thirdparty.jfx;

import com.mackenziehigh.socius.flow.Processor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

/**
 * Static Factories for common actors, etc, related to Java FX.
 */
public final class ActorsFx
{
    public static Processor<Boolean> createButtonListener (final MenuItem node)
    {
        final Processor<Boolean> processor = Processor.newProcessor(StageFx.instance());
        node.setOnAction(x -> processor.dataIn().send(true));
        return processor;
    }

    public static Processor<Boolean> createButtonListener (final RadioMenuItem node)
    {
        final Processor<Boolean> processor = Processor.newProcessor(StageFx.instance());
        node.setOnAction(x -> processor.dataIn().send(true));
        return processor;
    }

    public static Processor<Boolean> createButtonListener (final CheckMenuItem node)
    {
        final Processor<Boolean> processor = Processor.newProcessor(StageFx.instance());
        node.setOnAction(x -> processor.dataIn().send(true));
        return processor;
    }

    public static Processor<Boolean> createButtonListener (final Button node)
    {
        final Processor<Boolean> processor = Processor.newProcessor(StageFx.instance());
        node.setOnAction(x -> processor.dataIn().send(true));
        return processor;
    }

    public static Processor<Boolean> createButtonListener (final CheckBox node)
    {
        final Processor<Boolean> processor = Processor.newProcessor(StageFx.instance());
        node.setOnAction(x -> processor.dataIn().send(node.isSelected()));
        return processor;
    }

    public static Processor<Boolean> createButtonListener (final RadioButton node)
    {
        final Processor<Boolean> processor = Processor.newProcessor(StageFx.instance());
        node.setOnAction(x -> processor.dataIn().send(node.isSelected()));
        return processor;
    }

    public static Processor<Boolean> createButtonListener (final ToggleButton node)
    {
        final Processor<Boolean> processor = Processor.newProcessor(StageFx.instance());
        node.setOnAction(x -> processor.dataIn().send(node.isSelected()));
        return processor;
    }

    public static Processor<Double> createButtonListener (final Slider node)
    {
        final Processor<Double> processor = Processor.newProcessor(StageFx.instance());
        node.valueProperty().addListener((o, oldVal, newVal) -> processor.dataIn().send(newVal.doubleValue()));
        return processor;
    }

    public static Processor<String> createTextSetter (final Label node)
    {
        return Processor.newProcessor(StageFx.instance(), (String x) -> node.setText(x));
    }

    public static Processor<String> createTextSetter (final TextField node)
    {
        return Processor.newProcessor(StageFx.instance(), (String x) -> node.setText(x));
    }

    public static Processor<String> createTextSetter (final TextArea node)
    {
        return Processor.newProcessor(StageFx.instance(), (String x) -> node.setText(x));
    }

    public static Processor<String> createTextGetter (final TextField node)
    {
        final Processor<String> processor = Processor.newProcessor(StageFx.instance());
        node.textProperty().addListener((observable, oldVal, newVal) -> processor.dataIn().send(newVal));
        return processor;
    }

    public static Processor<String> createTextGetter (final TextArea node)
    {
        final Processor<String> processor = Processor.newProcessor(StageFx.instance());
        node.textProperty().addListener((observable, oldVal, newVal) -> processor.dataIn().send(newVal));
        return processor;
    }

    public static <T> Processor<SingleSelectionModel<T>> createSelectionListener (final ComboBox<T> node)
    {
        final Processor<SingleSelectionModel<T>> processor = Processor.newProcessor(StageFx.instance());
        node.setOnAction(x -> node.getSelectionModel());
        return processor;
    }

    public static <T> Processor<T> createSelectionListener (final ListView<T> node)
    {
        final Processor<T> processor = Processor.newProcessor(StageFx.instance());
        node.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> processor.dataIn().send(newVal));
        return processor;
    }

    public static Processor<Double> createSetter (final Slider node)
    {
        return Processor.newProcessor(StageFx.instance(), (Double x) -> node.setValue(x));
    }

    public static Processor<Double> createSetter (final ProgressBar node)
    {
        return Processor.newProcessor(StageFx.instance(), (Double x) -> node.setProgress(x));
    }

    public static Processor<Double> createSetter (final ProgressIndicator node)
    {
        return Processor.newProcessor(StageFx.instance(), (Double x) -> node.setProgress(x));
    }
}
