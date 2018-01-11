package Khip8

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class Chip8Disassembler {

    companion object {


        /**
         * Disassemble a chip8 rom file
         * @param filename Filename on the disk
         * @param withAddr Add opcode address to instructions
         * @return list of instructions
         */
        fun disassembleROM(filename: String, withAddr: Boolean = false): List<String> {
            val content: ByteArray

            try {
                val path: Path = Paths.get(filename)
                content = Files.readAllBytes(path)
            } catch (e: Exception) {
                throw Exception(e.javaClass.simpleName + ": " + e.message)
            }

            var instructions = mutableListOf<String>()

            var index: Int = 0

            while (index < content.size-1) {
                val b1: Int = content[index].toInt() and 0xFF
                val b2: Int = content[index + 1].toInt() and 0xFF
                val opc: Int = b1 shl 8 or b2

                val text = disassembleOpcode(opc)

                if (withAddr)
                    instructions.add("#" + String.format("0x%03X", index) + " : " + text + " (" + String.format("0x%04X", opc) + ")")
                else
                    instructions.add(text + " (" + String.format("0x%04X", opc) + ")")
                index += 2
            }

            return instructions
        }

        /**
         * Disassemble a single opcode
         *
         * opcodes references :
         * http://devernay.free.fr/hacks/chip8/C8TECH10.HTM#3.0
         * https://en.wikipedia.org/wiki/CHIP-8#Opcode_table
         * https://github.com/JamesGriffin/CHIP-8-Emulator/blob/master/src/chip8.cpp
         * https://github.com/trapexit/chip-8_documentation
         *
         * @param opc Opcode to disassemble
         * @return Representation of instruction in pseudocode
         */
        fun disassembleOpcode(opc: Int): String {
            var text: String = "unknown instruction"
            when (opc shr 12) {
                0x0 -> { // 0x00NN, ignoring 0x0NNN opcodes
                    val nn: Int = opc and 0xFF
                    when (nn) {
                        0xE0 -> text = "Clear Screen"
                        0xEE -> text = "Return"
                    }
                }
                0x1 -> { // 0x1NNN
                    val nnn = opc and 0xFFF
                    text = "Jump to " + String.format("0x%03X", nnn)
                }
                0x2 -> { // 0x2NNN
                    val nnn = opc and 0xFFF
                    text = "Call at " + String.format("0x%03X", nnn)
                }
                0x3 -> { // 0x3XNN
                    val x = opc and 0xFFF shr 8
                    val nn = opc and 0xFF
                    text = "if(V" + x.toString(16) + " == " + String.format("0x%02X", nn) + "), skip next instr."
                }
                0x4 -> { // 0x4XNN
                    val x = opc and 0xFFF shr 8
                    val nn = opc and 0xFF
                    text = "if(V" + x.toString(16) + " != " + String.format("0x%02X", nn) + "), skip next instr."
                }
                0x5 -> { // 0x5XY0
                    val x = opc and 0xFFF shr 8
                    val y = opc and 0xFF shr 4
                    text = "if(V" + x.toString(16) + " == V" + y.toString(16) + "), skip next instr."
                }
                0x6 -> { // 0x6XNN
                    val x = opc and 0xFFF shr 8
                    val nn = opc and 0xFF
                    text = "V" + x.toString(16) + " = " + String.format("0x%02X", nn)
                }
                0x7 -> { // 0x7XNN
                    val x = opc and 0xFFF shr 8
                    val nn = opc and 0xFF
                    text = "V" + x.toString(16) + " = V" + x.toString(16) + " + " + String.format("0x%02X", nn)
                }
                0x8 -> { // 0x8XY0 <- last number determines operation
                    val x = opc and 0xFFF shr 8
                    val y = opc and 0xFF shr 4
                    val op = opc and 0xF
                    when (op) {
                        0x0 -> text = "V" + x.toString(16) + " = V" + y.toString(16)
                        0x1 -> text = "V" + x.toString(16) + " OR V" + y.toString(16) + " (bitwise op)"
                        0x2 -> text = "V" + x.toString(16) + " AND V" + y.toString(16) + " (bitwise op)"
                        0x3 -> text = "V" + x.toString(16) + " XOR V" + y.toString(16)
                        0x4 -> text = "V" + x.toString(16) + " += V" + y.toString(16)
                        0x5 -> text = "V" + x.toString(16) + " -= V" + y.toString(16)
                        0x6 -> text = "V" + x.toString(16) + " = V" + y.toString(16) + " = V" + y.toString(16) + " >> 1"
                        0x7 -> text = "V" + x.toString(16) + " = V" + y.toString(16) + " - V" + x.toString(16)
                        0xE -> text = "V" + x.toString(16) + " = V" + y.toString(16) + " = V" + y.toString(16) + " << 1"
                    }
                }
                0x9 -> { // 0x9XY0
                    val x = opc and 0xFFF shr 8
                    val y = opc and 0xFF shr 4
                    val op = opc and 0xF
                    if (op == 0) text = "if(V" + x.toString(16) + " != V" + y.toString(16) + "), skip next instr."
                }
                0xA -> { // 0xANNN
                    val nnn = opc and 0xFFF
                    text = "I = " + String.format("0x%03X", nnn)
                }
                0xB -> { // 0xBNNN
                    val nnn = opc and 0xFFF
                    text = "PC = V0 + " + String.format("0x%03X", nnn)
                }
                0xC -> { // 0xCXNN
                    val x = opc and 0xFFF shr 8
                    val nn = opc and 0xFF
                    text = "V" + x.toString(16) + " = rand() AND " + String.format("0x%02X", nn) + " (bitwise op)"
                }
                0xD -> { // 0xDXYN
                    val x = opc and 0xFFF shr 8
                    val y = opc and 0xFF shr 4
                    val n = opc and 0xF
                    text = "Draw at (V" + x.toString(16) + ", V" + y.toString(16) + ") with height " + n.toString(16)
                }
                0xE -> { // 0xEX__
                    val x = opc and 0xFFF shr 8
                    val op = opc and 0xFF
                    when(op){
                        0x9E -> text = "if(key() == V" + x.toString(16) + "), skip next instr."
                        0xA1 -> text = "if(key() != V" + x.toString(16) + "), skip next instr."
                    }
                }
                0xF -> { // 0xFX__, where __ = op
                    val x = opc and 0xFFF shr 8
                    val op = opc and 0xFF
                    when (op) {
                        0x07 -> text = "V" + x.toString(16) + " = get_delay()"
                        0x0A -> text = "V" + x.toString(16) + " = get_key() (blocking operation)"
                        0x15 -> text = "Delay timer = V" + x.toString(16)
                        0x18 -> text = "Sound timer = V" + x.toString(16)
                        0x1E -> text = "I += V" + x.toString(16)
                        0x29 -> text = "I = sprite_addr(V" + x.toString(16) + ")"
                        0x33 -> text = "set_BCD(V" + x.toString(16) + ")"
                        0x55 -> text = "reg_dump(V" + x.toString(16) + ", &I)"
                        0x65 -> text = "reg_load(V" + x.toString(16) + ", &I)"
                    }
                }
                else -> {
                    text = "unknown instruction"
                }
            }
            return text
        }

    }

}