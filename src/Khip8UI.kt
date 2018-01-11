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
                g2d.drawLine(line, coln, line, coln)
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

class Chip8Display(rom: String) : JFrame() {
    private val SCREEN_WIDTH = 64
    private val SCREEN_HEIGHT = 32
    private val emu: Chip8Emu = Chip8Emu()
    private val surf: Surface = Surface(emu.getScreen())

    init {
        emu.loadRom(rom)
        initUI()
        emulate()
    }

    private fun initUI() {
        add(surf)

        title = "KHIP-8"
        setSize(SCREEN_WIDTH*2, SCREEN_HEIGHT*2)
        setLocationRelativeTo(null)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    }

    fun emulate() {
        while (true) {
            emu.emulateCycle()
            if(emu.getChip8Opcode() == 0)
                break;
            paintScreen()
        }
    }

    fun paintScreen() {
        val screen = emu.getScreen()
        surf.doPaint(screen)
    }
}