import Khip8.Chip8Emu
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.concurrent.timer

class Surface(chip8screen: Array<Array<Int>>) : JPanel() {
    private val SCREEN_HEIGHT = 32
    private val SCREEN_WIDTH = 64


    private var chip8screen: Array<Array<Int>> = chip8screen

    private fun doDrawing(g: Graphics?) {
        val g2d: Graphics2D = g as Graphics2D

        for (line in 0 until SCREEN_HEIGHT) {
            for (coln in 0 until SCREEN_WIDTH) {
                if (chip8screen[line][coln] == 0)
                    g2d.paint = Color.black
                else
                    g2d.paint = Color.white
                g2d.drawLine(coln, line, coln, line)
            }
        }
    }

    fun doPaint(screen: Array<Array<Int>>) {
        chip8screen = screen
        repaint()
    }

    override fun paintComponent(p0: Graphics?) {
        super.paintComponent(p0)
        doDrawing(p0)
    }
}

class Chip8Display(rom: String) : JFrame(), ActionListener {
    private val SCREEN_WIDTH = 64
    private val SCREEN_HEIGHT = 32
    private val CLOCK_SPEED = 16
    private val emu: Chip8Emu = Chip8Emu()
    private val surf: Surface = Surface(emu.getScreen())
    private val clock:Timer = Timer(CLOCK_SPEED, this)

    init {
        emu.loadRom(rom)
        initUI()
        emulate()
        clock.start()
    }

    private fun initUI() {
        add(surf)

        addWindowListener(object : WindowAdapter(){
            override fun windowClosed(p0: WindowEvent?) {
                super.windowClosed(p0)
                clock.stop()
            }
        })

        title = "KHIP-8"
        setSize(SCREEN_WIDTH*2, SCREEN_HEIGHT*3)
        setLocationRelativeTo(null)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    }

    fun emulate() {
        emu.emulateCycle()
        surf.doPaint(emu.getScreen())
    }

    override fun actionPerformed(p0: ActionEvent?) {
        emulate()
    }
}