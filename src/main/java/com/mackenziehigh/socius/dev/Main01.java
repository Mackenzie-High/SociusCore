package com.mackenziehigh.socius.dev;

import com.mackenziehigh.socius.utils.TrampolineMachine;
import java.util.Scanner;

/**
 *
 */
public class Main01
        implements TrampolineMachine
{
    String name;

    private State queryName ()
    {
        name = new Scanner(System.in).nextLine();
        return this::echoName;
    }

    private State echoName ()
    {
        System.out.println("Name = " + name);
        return this::queryName;
    }

    @Override
    public State initial ()
    {
        return this::queryName;
    }

    public static void main (String[] args)
    {
        new Main01().run();
    }
}
