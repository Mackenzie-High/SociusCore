# Socius 

Note: This project is still in alpha phase development.

Socius provides commonly used actor types based on the minimalist [Cascade](https://github.com/Mackenzie-High/Cascade) framework. 

## Related Projects:

* [Socius - Core](https://github.com/Mackenzie-High/SociusCore): Base Components. 
* [Socius - Nats](https://github.com/Mackenzie-High/SociusNats): Provides the ability to connect to [NATS-based](https://nats.io/) message-oriented-middleware.
* [Socius - Web](https://github.com/Mackenzie-High/SociusWeb): Provides a server for creating HTTP-based RESTful micro-services. 

## Compilation and Installation

**Step #1: Install Cascade**

```bash
git clone 'https://github.com/Mackenzie-High/Cascade.git'
cd Cascade/
git checkout REL_CASCADE_2_0_1_BETA
mvn clean install
```

**Step #2: Install Socius Core Itself**
```bash
git clone 'https://github.com/Mackenzie-High/SociusCore.git'
cd SociusCore/
git checkout REL_SOCIUS_CORE_1_0_1_ALPHA
mvn clean install
```

## Core Classes:
* [AbstractPipeline](/documentation/AbstractPipeline.md)
* [AbstractProcessor](/documentation/AbstractProcessor.md)
* [AbstractPushdownAutomaton](/documentation/AbstractPushdownAutomaton.md)
* [AbstractTrampoline](/documentation/AbstractTrampoline.md)
* [AsyncTestTool](/documentation/AsyncTestTool.md)
* [Bus](/documentation/Bus.md)
* [Clock](/documentation/Clock.md)
* [DelayedSender](/documentation/DelayedSender.md)
* [Duplicator](/documentation/Duplicator.md)
* [Fanout](/documentation/Fanout.md)
* [Funnel](/documentation/Funnel.md)
* [IfElse](/documentation/IfElse.md)
* [LookupSwitch](/documentation/LookupSwitch.md)
* [LookupTower](/documentation/LookupTower.md)
* [Minuteman](/documentation/Minuteman.md)
* [Oscillator](/documentation/Oscillator.md)
* [Pipeline](/documentation/Pipeline.md)
* [Printer](/documentation/Printer.md)
* [Processor](/documentation/Processor.md)
* [Requester](/documentation/Requester.md)
* [RequestTower](/documentation/RequestTower.md)
* [RoundRobin](/documentation/RoundRobin.md)
* [Router](/documentation/Router.md)
* [Sink](/documentation/Sink.md)
* [Source](/documentation/Source.md)
* [TableSwitch](/documentation/TableSwitch.md)
* [TableTower](/documentation/TableTower.md)
* [TypeCaster](/documentation/TypeCaster.md)
* [Valve](/documentation/Valve.md)
* [Variable](/documentation/Variable.md)
* [WeightBalancer](/documentation/WeightBalancer.md)
