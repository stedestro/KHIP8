package Khip8

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.concurrent.timerTask


class Chip8Emu {
    // consts
    private val RAM_SIZE = 0x1000
    private val SCREEN_WIDTH = 64
    private val SCREEN_HEIGHT = 32
    private val SCREEN_SIZE = SCREEN_WIDTH * SCREEN_HEIGHT
    private val PROGRAM_START = 0x200

    //registers
    private var V: IntArray = IntArray(16, { 0 })
    private var PC: Int = 0x200
    private var SP: Int = 0
    private var I: Int = 0x0
    private var DT: Int = 0
    private var ST: Int = 0
    private var stack: Stack<Int> = Stack()

    // RAM & SCREEN
    private var key: IntArray = IntArray(16, { 0 })
    private var memory: IntArray = IntArray(RAM_SIZE, { 0 })
    private var screen: Array<Array<Int>> = Array(SCREEN_HEIGHT, { Array(SCREEN_WIDTH, { 0 }) })

    private var opcode: Int = 0x0

    public var shouldDraw:Boolean = false

    /**
     * Secondary constructor:
     *     Load font data
     *     ...
     */
    init {
        val fonts = createFontSprite()
        for (byte in fonts.indices)
            memory[byte] = fonts[byte]
    }

    /**
     * Copy file to chip8 ram at index 0x200
     */
    fun loadRom(filename: String) {
        val content: ByteArray
        try {
            val path: Path = Paths.get(filename)
            content = Files.readAllBytes(path)
        } catch (e: Exception) {
            throw Exception(e.javaClass.simpleName + ": " + e.message)
        }

        for (byteIndex in content.indices)
            memory[PROGRAM_START + byteIndex] = content[byteIndex].toInt() and 0xff
    }

    fun emulateCycle() {
        // fetch opcode
        opcode = memory[PC] shl 8 or memory[PC + 1]

        when (opcode shr 12) {
            0x0 -> {
                val nn: Int = opcode and 0xFF
                when (nn) {
                    0x00 -> {
                        return
                    }
                    0xE0 -> { // CLS
                        screen = Array(SCREEN_HEIGHT, { Array(SCREEN_WIDTH, { 0 }) })
                        PC += 2
                    }
                    0xEE -> { // RET
                        PC = stack.pop()
                    }
                }
            }
            0x1 -> { // JP addr
                PC = opcode and 0xFFF
            }
            0x2 -> { // CALL addr
                stack.push(PC)
                PC = opcode and 0xFFF
            }
            0x3 -> { // SE Vx, byte
                val x = opcode and 0xFFF shr 8
                val nn = opcode and 0xFF
                if (V[x] == nn)
                    PC += 4
                else
                    PC += 2
            }
            0x4 -> { // SNE Vx, byte
                val x = opcode and 0xFFF shr 8
                val nn = opcode and 0xFF
                if (V[x] != nn)
                    PC += 4
                else
                    PC += 2
            }
            0x5 -> { // SE Vx, Vy
                val x = opcode and 0xFFF shr 8
                val y = opcode and 0xFF shr 4
                if (V[x] == V[y])
                    PC += 4
                else
                    PC += 2
            }
            0x6 -> { // LD Vx, byte
                val x = opcode and 0xFFF shr 8
                val nn = opcode and 0xFF
                V[x] = nn
                PC += 2
            }
            0x7 -> { // ADD Vx, byte
                val x = opcode and 0xFFF shr 8
                val nn = opcode and 0xFF
                V[x] = (V[x] + nn) and 0xFF
                PC += 2
            }
            0x8 -> {
                val x = opcode and 0xFFF shr 8
                val y = opcode and 0xFF shr 4
                val op = opcode and 0xF
                when (op) {
                    0x0 -> V[x] = V[y]              // LD Vx, Vy
                    0x1 -> V[x] = V[x] or V[y]      // OR Vx, Vy
                    0x2 -> V[x] = V[x] and V[y]     // AND Vx, Vy
                    0x3 -> V[x] = V[x] xor V[y]     // XOR Vx, Vy
                    0x4 -> {                        // ADD Vx, Vy -- Vf = 1 if carry, else 0
                        if ((V[x] + V[y]) > 0xFF) V[0xF] = 1 else V[0xF] = 0
                        V[x] = (V[x] + V[y]) and 0xFF
                    }
                    0x5 -> {                        // SUB Vx, Vy -- Vf = 1 if NOT borrow, else 1
                        if ((V[x] > V[y])) V[0xF] = 1 else V[0xF] = 0
                        V[x] = (V[x] - V[y]) and 0xFF
                    }
                    0x6 -> {                        // SHR Vx {, Vy}
                        if ((V[x] and 1) == 1) V[0xF] = 1 else V[0xF] = 0
                        V[x] = (V[x] shr 1) and 0xFF
                        V[y] = V[x]
                    }
                    0x7 -> {                        // SUBN Vx, Vy
                        if (V[y] > V[x]) V[0xF] = 1 else V[0xF] = 0
                        V[x] = (V[y] - V[x]) and 0xFF
                    }
                    0xE -> {                        // SHL Vx {, Vy}
                        if (((V[x] shr 7) and 1) == 1) V[0xF] = 1 else V[0xF] = 0
                        V[x] = (V[x] shl 1) and 0xFF
                        V[y] = V[x]
                    }
                }
                PC += 2
            }
            0x9 -> { // SNE Vx, Vy
                val x = opcode and 0xFFF shr 8
                val y = opcode and 0xFF shr 4
                if (V[x] != V[y])
                    PC += 4
                else
                    PC += 2
            }
            0xA -> { // LD I, addr
                I = opcode and 0xFFF
                PC += 2
            }
            0xB -> { // JP V0, addr
                val nnn = opcode and 0xFFF
                PC = (V[0] + nnn) and 0xFFF
            }
            0xC -> { // RND Vx, byte
                val x = opcode and 0xFFF shr 8
                val nn = opcode and 0xFF
                val rnd = ThreadLocalRandom.current().nextInt(0x0, 0xFF + 1)
                V[x] = rnd and nn
                PC += 2
            }
            0xD -> { // Draw Vx, Vy, nibble
                val coln = opcode and 0xFFF shr 8
                val line = opcode and 0xFF shr 4
                val n = opcode and 0xF
                V[0xF] = 0

                for (spriteByteIndex in 0 until n) {
                    val byte = memory[I + spriteByteIndex]
                    var pixIndex: Int = 0
                    for (pixel in 7 downTo 0) {
                        val bit = (byte shr pixel) and 1
                        if (bit == 1) { // if bit is a visible pixel
                            val screenCol = (V[coln] + pixIndex) % SCREEN_WIDTH
                            val screenLin = (V[line] + spriteByteIndex) % SCREEN_HEIGHT
                            val pixOnScreen = screen[screenLin][screenCol]
                            if (pixOnScreen == 1) {
                                V[0xF] = 1
                            }
                            screen[screenLin][screenCol] = screen[screenLin][screenCol] xor bit
                        }
                        pixIndex++
                    }
                }
                PC += 2
                shouldDraw = true
            }
            0xE -> {
                val x = opcode and 0xFFF shr 8
                val op = opcode and 0xFF
                when (op) {
                    0x9E -> { // SKP Vx
                        if (key[V[x]] != 0)
                            PC += 4
                        else
                            PC += 2
                    }
                    0xA1 -> { // SKPN Vx
                        if (key[V[x]] == 0)
                            PC += 4
                        else
                            PC += 2
                    }
                }
            }
            0xF -> {
                val x = opcode and 0xFFF shr 8
                val op = opcode and 0xFF

                when (op) {
                    0x07 -> { // LD Vx, DT
                        V[x] = DT
                        PC += 2
                    }
                    0x0A -> { // LD Vx, K
                        var keyPress: Boolean = false

                        for (i in key.indices) {
                            if (key[i] != 0) {
                                keyPress = true
                                V[x] = i and 0xFF
                                break
                            }
                        }

                        if (!keyPress) // do nothing if no key is pressed
                            return

                        PC += 2
                    }
                    0x15 -> { // LD DT, Vx
                        DT = V[x]
                        // <- START DELAY TIMER HERE!!!!
                        PC += 2
                    }
                    0x18 -> { // LD ST, Vx
                        ST = V[x]
                        // <- START SOUND TIMER HERE!!!!
                        PC += 2
                    }
                    0x1E -> { // ADD I, Vx
                        I = (I + V[x]) and 0xFFF
                        PC += 2
                    }
                    0x29 -> { // LD F, Vx

                    }
                    0x33 -> { // LD B, Vx
                        memory[I] = V[x] / 100
                        memory[I + 1] = V[x] / 10 % 10
                        memory[I + 2] = V[x] % 100 % 10
                        PC += 2
                    }
                    0x55 -> { // LD [I], Vx
                        for (i in 0..x) {
                            memory[I] = V[i]
                            I++
                        }
                        PC += 2
                    }
                    0x65 -> { // LD Vx, [I]
                        for (i in 0..x) {
                            V[i] = memory[I]
                            I++
                        }
                        PC += 2
                    }
                }
            }
            else -> {
                throw Exception("Unknown or unimplemented opcode " + String.format("0x%04X", opcode))
            }
        }
    }

    fun getRegisters():IntArray {
        return V
    }

    fun getScreen(): Array<Array<Int>> {
        return screen
    }

    fun getChip8Opcode():Int {
        return opcode
    }

    fun getChip8OpcodeString(): String {
        return String.format("0x%04X", opcode)
    }

    fun getStackSize():Int {
        return stack.size
    }

    fun createFontSprite(): IntArray {
        val fontArray: IntArray = intArrayOf(
                0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
                0x20, 0x60, 0x20, 0x20, 0x70, // 1
                0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
                0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
                0x90, 0x90, 0xF0, 0x10, 0x10, // 4
                0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
                0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
                0xF0, 0x10, 0x20, 0x40, 0x40, // 7
                0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
                0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
                0xF0, 0x90, 0xF0, 0x90, 0x90, // A
                0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
                0xF0, 0x80, 0x80, 0x80, 0xF0, // C
                0xE0, 0x90, 0x90, 0x90, 0xE0, // D
                0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
                0xF0, 0x80, 0xF0, 0x80, 0x80  // F
        )
        return fontArray
    }

    fun printRam(size: Int, start:Int=0x200) {
        for (x in 0 until size)
            print(memory[start + x].toString(16) + " ")
        println()
    }

    fun printRegisters(start:Int=0, end:Int=0xf) {
        for (reg in start..end)
            print("V" + reg.toString(16) + ":" + String.format("%02X", V[reg]) + " ")
        println()
        print("PC:" + String.format("0x%04X",PC) + " I:" + String.format("0x%04X",I) + "\n")
    }
}