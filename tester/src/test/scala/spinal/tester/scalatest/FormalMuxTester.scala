package spinal.tester.scalatest

import spinal.core._
import spinal.core.formal._
import spinal.lib._
import spinal.lib.formal._

class FormalMuxTester extends SpinalFormalFunSuite {
  def formalmux(selWithCtrl: Boolean = false) = {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      // .withDebug
      .doVerify(new Component {
        val portCount = 5
        val dataType = Bits(8 bits)
        val dut = FormalDut(new StreamMux(dataType, portCount))

        val reset = ClockDomain.current.isResetActive

        assumeInitial(reset)

        val muxSelect = anyseq(UInt(log2Up(portCount) bit))
        val muxInputs = Vec(slave(Stream(dataType)), portCount)
        val muxOutput = master(Stream(dataType))

        dut.io.select := muxSelect
        muxOutput << dut.io.output

        assumeInitial(muxSelect < portCount)
        val selStableCond = if (selWithCtrl) past(muxOutput.isStall) else null

        when(reset || past(reset)) {
          for (i <- 0 until portCount) {
            assume(muxInputs(i).valid === False)
          }
        }

        if (selWithCtrl) {
          cover(selStableCond)
          when(selStableCond) {
            assume(stable(muxSelect))
          }
        }
        muxOutput.withAsserts()
        muxOutput.withCovers(5)

        for (i <- 0 until portCount) {
          muxInputs(i) >> dut.io.inputs(i)
          muxInputs(i).withAssumes()
        }

        cover(muxOutput.fire)

        for (i <- 0 until portCount) {
          cover(dut.io.select === i)
          muxInputs(i).withAssumes()
        }

        when(muxSelect < portCount) {
          assert(muxOutput === muxInputs(muxSelect))
        }
      })
  }
  test("mux_sel_with_control") {
    formalmux(true)
  }
  test("mux_sel_without_control") {
    shouldFail(formalmux(false))
  }
}
