package embedding;

/**
 *
 * @author Hendrik
 */
public class Block {
    private char[][][] block;
    private final int blockSize = 8;
    private double complexity = 0;
    public int lsbLayer = 0;
    public boolean message = false;
    public boolean authentic = true;
    public boolean conjugated = false;
    public boolean isComplex = false;

    public Block() {
        this.block = new char[3][blockSize][blockSize];
    }
    
    public void calculateComplexity(){
        int borderLength = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 7; j++) {
                //red
                char current = block[0][i][j];
                char right = block[0][i][j+1];
                
                if(current != right)
                    borderLength++;  
                
                //green
                current = block[1][i][j];
                right = block[1][i][j+1];
                
                if(current != right)
                    borderLength++;  
                
                //blue
                current = block[2][i][j];
                right = block[2][i][j+1];
                
                if(current != right)
                    borderLength++;  
            }
        }
        
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 8; j++) {
                
                //red
                char current = block[0][i][j];
                char bottom = block[0][i+1][j];
                
                if(current != bottom)
                    borderLength++;
                
                //green
                current = block[1][i][j];
                bottom = block[1][i+1][j];
                
                if(current != bottom)
                    borderLength++; 
                
                //blue
                current = block[2][i][j];
                bottom = block[2][i+1][j];
                
                if(current != bottom)
                    borderLength++; 
            }
        }
        //double maxBorderLength = 2*(Math.pow(2, 8)*(Math.pow(2, 8)-1));
        
        
        //Check formula for max border complexity
        //* 3 is for the red,green and blue
        complexity =borderLength/(64.0*3);
        //System.out.println("Complexity: "+complexity);
    }

    public char[][][] getBlock() {
        return block;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlock(char[][][] block) {
        this.block = block;
    }

    public double getComplexity() {
        return complexity;
    }
    
    
    
}
