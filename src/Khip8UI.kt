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

class Surface(chip8screen: Array<Array<Int>>, screenScaling: Int) : JPanel() {
    private val SCREEN_HEIGHT = 32
    private val SCREEN_WIDTH = 64

    private var screenScaling: Int = screenScaling
    private var chip8screen: Array<Array<Int>> = chip8screen

    private fun doDrawing(g: Graphics?) {
        val g2d: Graphics2D = g as Graphics2D

        for (line in 0 until SCREEN_HEIGHT * screenScaling) {
            for (coln in 0 until SCREEN_WIDTH * screenScaling) {
                if(line > SCREEN_HEIGHT*screenScaling)
                    throw Exception("Line is $line")
                else if (coln > SCREEN_WIDTH*screenScaling)
                    throw Exception("Coln is $coln")
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

class Chip8Display(rom: String, screenScaling: Int = 3) : JFrame() {

    private val SCREEN_WIDTH = 64
    private val SCREEN_HEIGHT = 32
    private val CLOCK_SPEED = 16
    private val emu: Chip8Emu = Chip8Emu()
    private val surf: Surface = Surface(emu.getScreen(), screenScaling)
    private val clock: Timer = Timer(CLOCK_SPEED, object : ActionListener {
        override fun actionPerformed(p0: ActionEvent?) {
            emulate()
        }
    })
    private val screenScaling: Int = screenScaling

    init {
        emu.loadRom(rom)
        initUI()
        emulate()
        clock.start()
    }

    private fun initUI() {
        add(surf)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(p0: WindowEvent?) {
                super.windowClosed(p0)
                clock.stop()
            }
        })


        title = "KHIP8"
        setSize(SCREEN_WIDTH * screenScaling + 16, SCREEN_HEIGHT * screenScaling + 39)
        setLocationRelativeTo(null)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    }

    fun emulate() {
        for (i in 0 until 32)
            emu.emulateCycle()
        if(emu.shouldDraw){
            if (screenScaling != 1)
                surf.doPaint(rescaleScreen(emu.getScreen(), screenScaling, screenScaling))
            else
                surf.doPaint(emu.getScreen())
            emu.shouldDraw = false
        }
    }

    fun rescaleScreen(screen: Array<Array<Int>>, scalex: Int, scaley: Int): Array<Array<Int>> {
        val rows: Int = screen.size * scaley
        val cols: Int = screen[0].size * scalex
        var out: Array<Array<Int>> = Array(rows, { Array(cols, { 0 }) })
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                out[r][c] = screen[r / scaley][c / scalex]
            }
        }
        return out
    }
}