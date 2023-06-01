package hardwar.branch.prediction.judged.PAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAg implements BranchPredictor {
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PABHR; // per address branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table

    public PAg() {
        this(4, 2, 8);
    }

    /**
     * Creates a new PAg predictor with the given BHR register size and initializes the PABHR based on
     * the branch instruction size and BHR size
     *
     * @param BHRSize               the size of the BHR register
     * @param SCSize                the size of the register which hold the saturating counter value
     * @param branchInstructionSize the number of bits which is used for saving a branch instruction
     */
    public PAg(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        // Initialize the PABHR with the given bhr and branch instruction size
        this.PABHR = new RegisterBank(BHRSize, branchInstructionSize);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        this.PHT = new PageHistoryTable((int)Math.pow(2,BHRSize), SCSize);

        // Initialize the SC register
        this.SC = new SIPORegister("SC", SCSize, null);
    }

    /**
     * @param instruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction instruction) {
        // TODO: complete Task 1
        ShiftRegister PHTAddress = this.PABHR.read(instruction.getOpcode());
        PHT.putIfAbsent(PHTAddress.read(), getDefaultBlock());
        Bit[] SCBits = PHT.get(PHTAddress.read());
        Bit msb = SCBits[0];
        if(msb==Bit.ZERO) return BranchResult.NOT_TAKEN;
        else return BranchResult.TAKEN;
    }

    /**
     * @param instruction the branch instruction
     * @param actual      the actual result of branch (taken or not)
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        ShiftRegister PHTAddress = this.PABHR.read(instruction.getOpcode());
        Bit[] SCBits = PHT.get(PHTAddress.read());
        Bit[] result;
        if(actual == BranchResult.TAKEN)
            result = CombinationalLogic.count(SCBits, true,CountMode.SATURATING);
        else
            result = CombinationalLogic.count(SCBits, false,CountMode.SATURATING);
        PHT.put(PHTAddress.read(), result);
        Bit[] newBits = PHTAddress.read();
        for (int i = newBits.length - 1; i > 0; i--)
            newBits[i] = newBits[i - 1];

        // Insert the new bit at the beginning of the register
        newBits[0] = actual == BranchResult.TAKEN ? Bit.ONE : Bit.ZERO;
//        PHTAddress.insert(actual == BranchResult.TAKEN ? Bit.ONE : Bit.ZERO);
        PABHR.write(instruction.getOpcode(), newBits);
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "PAg predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
