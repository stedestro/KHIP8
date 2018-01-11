import Khip8.Chip8Disassembler
import Khip8.Chip8Emu


fun main(args: Array<String>) {

    if(args.size < 2)
        throw Exception("Not enough arguments")

    when(args[0]){
        "--disassembly", "-d" -> {
            var dis:List<String>
            val wa = if("with_addr" in args) true else false
            dis = Chip8Disassembler.disassembleROM(args[1], withAddr = wa)
            for (instr in dis)
                println(instr)
        }
        "--emulate", "-e" -> {
            val ui:Chip8Display = Chip8Display(args[1], screenScaling = 4)
            ui.isVisible = true
        }
        "--debug", "-d" -> {
            var emu = Chip8Emu()
            emu.loadRom(args[1])
            println("Init state\nRam @ 0x200 : ")
            emu.printRam(10)
            emu.printRegisters()
            var cycles:Int = 0
            do{
                emu.emulateCycle()
                if(emu.getChip8Opcode() == 0)
                    break
                println("\nResult for cycle $cycles")
                emu.printRegisters()
                println("Executed " + emu.getChip8OpcodeString())
                cycles++
            } while(emu.getChip8Opcode() != 0x0)
        }
        else -> println("Unknown flag \"" + args[0] + "\"")
    }
}