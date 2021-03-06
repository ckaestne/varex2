package edu.cmu.cs.varex

import java.util.logging.{ConsoleHandler, Level, Logger}

import com.caucho.quercus.TQuercus
import de.fosd.typechef.conditional.ConditionalLib
import de.fosd.typechef.featureexpr.{FeatureExpr, FeatureExprFactory}
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch
import org.junit.Assert

import scala.collection.JavaConverters._


/**
  * Test infrastructure for variational execution of PHP
  */
trait AbstractPhpTest extends ConditionalOutputInfrastructure {

    type TOpt[T] = de.fosd.typechef.conditional.Opt[T]

    val log = Logger.getLogger("com.caucho.quercus")
    log.setLevel(Level.ALL)
    val handler = new ConsoleHandler()
    log.addHandler(handler)
    handler.setLevel(Level.WARNING)

    log.fine("test")
    val ctx = FeatureExprFactory.createDefinedExternal("CTX")

    case class Eval(code: String) {



        def to(expected: ConditionalOutput): Unit = {
            //first run test without context
            val result = TQuercus.executeScript(code, VHelper.True())
            compare(toTypeChef(expected.toOptList), toTypeChef(result.asScala.toList))

            //then repeat test with a fixed context
            val ctxresult = TQuercus.executeScript(code, ctx)
            compare(toTypeChef(expected.toOptList.map(o => Opt.create(o.getCondition and ctx, o.getValue))), toTypeChef(ctxresult.asScala.toList))

        }

        def toError(): Unit = {
            try {
                val result = TQuercus.executeScript(code, ctx)
                Assert.fail("expected error, but execution succeeded with result "+toTypeChef(result.asScala.toList))
            } catch {
                case e: Exception =>
                case e: AssertionError =>
            }
        }
    }


    def eval(code: String) = {
        var script = "<?php "
        if (code contains "$FOO")
            script += "$FOO = create_conditional('foo');"
        if (code contains "$BAR")
            script += "$BAR = create_conditional('bar');"
        Eval(script + code)
    }


    private def toTypeChef(l: List[Opt[String]]): List[TOpt[String]] =
       l.map(o => de.fosd.typechef.conditional.Opt(o.getCondition, o.getValue))


    private def compare(expected: List[TOpt[String]], result: List[TOpt[String]]): Unit = {
        def compareOne(ctx: FeatureExpr, e: String, f: String): Unit =
            assert(ctx.isContradiction || e.trim==f.trim, s"mismatch between expected output and actual output in context $ctx: \nEXPECTED:\n$e\nFOUND:\n$f\nALL:\n"+render(result))

        val allExpected = ConditionalLib.explodeOptList(expected).map(_.mkString)
        val allFound = ConditionalLib.explodeOptList(result).map(_.mkString)
        ConditionalLib.vmapCombination(allExpected, allFound, FeatureExprFactory.True, compareOne)
    }

    private def render(result: List[TOpt[String]]): String =
        result.map(o => "[#condition " + o.feature + "]" + o.entry).mkString


    private  def explainResult(expected: String, actual: String): String = {
        val diff = new DiffMatchPatch()
        diff.patchMargin = 100
        val diffs = diff.diffMain(expected, actual, true)
        val patch = diff.patchMake(diffs)

        val txt = diff.patchToText(patch)

        s"mismatch between expected output and actual output: \nEXPECTED:\n$expected\nFOUND:\n$actual\nDIFF:\n$txt"
    }
}

trait ConditionalOutputInfrastructure {

    sealed trait ConditionalOutput {
        def ~(that: ConditionalOutput) = new ConcatOutput(this, that)
        def toOptList: List[Opt[String]]
    }

    case class OptionalOutput(f: FeatureExpr, output: String) extends ConditionalOutput {
        override def toString = output
        override def toOptList: List[Opt[String]] = List(new OptImpl(f, output))
    }

    case class ConcatOutput(a: ConditionalOutput, b: ConditionalOutput) extends ConditionalOutput {
        override def toString = a.toString+b.toString
        override def toOptList: List[Opt[String]] = a.toOptList ++ b.toOptList
    }


    def c(f: FeatureExpr, output: String): ConditionalOutput = new OptionalOutput(f, output)
    def o(output: String): ConditionalOutput = new OptionalOutput(FeatureExprFactory.True, output)

    def c(f: FeatureExpr, output: ConditionalOutput): ConditionalOutput = output match {
        case OptionalOutput(c, o) => new OptionalOutput(f and c, o)
        case ConcatOutput(a, b) => new ConcatOutput(c(f, a), c(f, b))
    }

    implicit def _toOutput(v: String): ConditionalOutput = o(v)

}