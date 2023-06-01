package hardwar.branch.prediction.judged.GAg;
import java.lang.Math;
import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

import javax.naming.spi.DirStateFactory.Result;

public class GAg implements BranchPredictor {
    private final ShiftRegister BHR; // branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table
    private final ShiftRegister SC; // saturated counter register

    public GAg() {
        this(4, 2);
    }

    /**
     * Creates a new GAg predictor with the given BHR register size and initializes the BHR and PHT.
     *
     * @param BHRSize the size of the BHR register
     * @param SCSize  the size of the register which hold the saturating counter value and the cache block size
     */
    public GAg(int BHRSize, int SCSize) {
        // TODO : complete the constructor
        // Initialize the BHR register with the given size and no default value
       
        this.BHR = new SIPORegister("BHR", BHRSize,null) ;

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PHT = new PageHistoryTable((int)Math.pow(2,BHRSize), SCSize);

        // Initialize the SC register
        SC = new SIPORegister("SC", SCSize, null);
    }

    /**
     * Predicts the result of a branch instruction based on the global branch history
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO : complete Task 1
        Bit[] bits = this.BHR.read();
        PHT.putIfAbsent(bits, getDefaultBlock());
        Bit[] SCbits = PHT.get(bits);
        Bit msb = SCbits[0];
        if(msb==Bit.ZERO)
            return BranchResult.NOT_TAKEN;
        else
            return BranchResult.TAKEN;
        
    }

    /**
     * Updates the values in the cache based on the actual branch result
     *
     * @param instruction the branch instruction
     * @param actual      the actual result of the branch condition
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO: complete Task 2
        Bit[] bhrBits = this.BHR.read();
        Bit[] SCbits = PHT.get(bhrBits);
        Bit[] result;
        if(actual == BranchResult.TAKEN)
            result = CombinationalLogic.count(SCbits, true,CountMode.SATURATING);
        else
            result = CombinationalLogic.count(SCbits, false,CountMode.SATURATING);
        PHT.put(bhrBits, result);
        BHR.insert(actual == BranchResult.TAKEN ? Bit.ONE : Bit.ZERO);

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
        return "GAg predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
