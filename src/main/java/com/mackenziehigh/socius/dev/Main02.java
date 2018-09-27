package com.mackenziehigh.socius.dev;

import com.mackenziehigh.socius.flow.Mapper;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.thirdparty.jfx.ActorsFx;
import com.mackenziehigh.socius.thirdparty.jfx.StageFx;
import java.time.Instant;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main02
        extends Application
{
    public static void main (String[] args)
    {
        launch(args);
    }

    @Override
    public void start (Stage primaryStage)
    {
        primaryStage.setTitle("Hello World!");
        Button btn = new Button();
        TextField field = new TextField();
        btn.setText("Say 'Hello World'");
        CheckBox cbox = new CheckBox("Launch");
        final Processor<Boolean> p1 = ActorsFx.createButtonListener(btn);
        final Processor<String> p2 = ActorsFx.createTextSetter(field);
        final Mapper<Boolean, String> p3 = Mapper.newMapper(StageFx.instance(), x -> "Vulcan + " + Instant.now());
        final Processor<Boolean> p4 = ActorsFx.createButtonListener(cbox);
        final Mapper<Boolean, String> p5 = Mapper.newMapper(StageFx.instance(), x -> "Andoria + " + x);

        p1.dataOut().connect(p3.dataIn());
        p3.dataOut().connect(p2.dataIn());

        p4.dataOut().connect(p5.dataIn());
        p5.dataOut().connect(p2.dataIn());

        VBox root = new VBox();
        root.getChildren().add(btn);
        root.getChildren().add(field);
        root.getChildren().add(cbox);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }
}
