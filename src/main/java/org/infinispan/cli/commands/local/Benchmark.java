package org.infinispan.cli.commands.local;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.infinispan.cli.benchmark.BenchmarkOutputFormat;
import org.infinispan.cli.benchmark.HotRodBenchmark;
import org.infinispan.cli.benchmark.HttpBenchmark;
import org.infinispan.cli.commands.CLI;
import org.infinispan.cli.completers.CacheCompleter;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.0
 **/
@CommandLine.Command(name = "benchmark", description = "Benchmarks server performance")
public class Benchmark implements Callable<Integer> {
   @CommandLine.ParentCommand
   private CLI cli;

   @CommandLine.Parameters(description = "Specifies the URI of the server to benchmark. Supported protocols are http, https, hotrod, hotrods. If you do not set a protocol, the benchmark uses the URI of the current connection.")
   String uri;

   @CommandLine.Option(names = {"-t", "--threads"}, defaultValue = "10", description = "Specifies the number of threads to create. Defaults to 10.")
   int threads;

   @CommandLine.Option(names = "--mode", defaultValue = "Throughput", description = "Specifies the benchmark mode. Possible values are Throughput, AverageTime, SampleTime, SingleShotTime, and All. Defaults to Throughput.")
   Mode mode;

   @CommandLine.Option(names = "--verbosity", defaultValue = "NORMAL", description = "Specifies the verbosity level of the output. Possible values, from least to most verbose, are SILENT, NORMAL, and EXTRA. Defaults to NORMAL.")
   VerboseMode verbosity;

   @CommandLine.Option(names = {"-c", "--count"}, defaultValue = "5", description = "Specifies how many measurement iterations to perform. Defaults to 5.")
   int count;

   @CommandLine.Option(names = "--time", defaultValue = "10s", description = "Sets the amount of time, in seconds, that each iteration takes. Defaults to 10.")
   String time;

   @CommandLine.Option(defaultValue = "5", names = "--warmup-count", description = "Specifies how many warmup iterations to perform. Defaults to 5.")
   int warmupCount;

   @CommandLine.Option(defaultValue = "1s", names = "--warmup-time", description = "Sets the amount of time, in seconds, that each warmup iteration takes. Defaults to 1.")
   String warmupTime;

   @CommandLine.Option(defaultValue = "MICROSECONDS", names = "--time-unit", description = "Specifies the time unit for results in the benchmark report. Possible values are NANOSECONDS, MICROSECONDS, MILLISECONDS, and SECONDS. The default is MICROSECONDS.")
   TimeUnit timeUnit;

   @CommandLine.Option(names = "--cache", completionCandidates = CacheCompleter.class, defaultValue = "benchmark", description = "Names the cache against which the benchmark is performed. Defaults to 'benchmark'.")
   String cache;

   @CommandLine.Option(defaultValue = "16", names = "--key-size", description = "Sets the size, in bytes, of the key. Defaults to 16 bytes.")
   int keySize;

   @CommandLine.Option(defaultValue = "1000", names = "--value-size", description = "Sets the size, in bytes, of the value. Defaults to 1000 bytes.")
   int valueSize;

   @CommandLine.Option(defaultValue = "1000", names = "--keyset-size", description = "Defines the size, in bytes, of the test key set. Defaults to 1000.")
   int keySetSize;

   @Override
   public Integer call() {
      OptionsBuilder opt = new OptionsBuilder();
      if (this.uri == null) {
         if (cli.context().isConnected()) {
            this.uri = cli.context().client().getConfiguration().toURI();
         } else {
            throw new IllegalArgumentException("You must specify a URI");
         }
      }
      URI uri = URI.create(this.uri);
      switch (uri.getScheme()) {
         case "hotrod":
         case "hotrods":
            opt.include(HotRodBenchmark.class.getSimpleName());
            break;
         case "http":
         case "https":
            opt.include(HttpBenchmark.class.getSimpleName());
            break;
         default:
            throw new IllegalArgumentException("Unknown scheme " + uri.getScheme());
      }
      opt
            .forks(0)
            .threads(threads)
            .param("uri", this.uri)
            .param("cacheName", this.cache)
            .param("keySize", Integer.toString(this.keySize))
            .param("valueSize", Integer.toString(this.valueSize))
            .param("keySetSize", Integer.toString(this.keySetSize))
            .mode(mode)
            .verbosity(verbosity)
            .measurementIterations(count)
            .measurementTime(TimeValue.fromString(time))
            .warmupIterations(warmupCount)
            .warmupTime(TimeValue.fromString(warmupTime))
            .timeUnit(timeUnit);
      try {
         new Runner(opt.build(), new BenchmarkOutputFormat(cli.context().out(), verbosity)).run();
         return 0;
      } catch (RunnerException e) {
         cli.context().printError(e);
         return 1;
      }
   }
}
