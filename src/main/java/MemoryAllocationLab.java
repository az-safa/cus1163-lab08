import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MemoryAllocationLab {

    // Represents a contiguous block of memory
    static class MemoryBlock {
        int start;
        int size;
        String processName; // null if the block is free

        public MemoryBlock(int start, int size, String processName) {
            this.start = start;
            this.size = size;
            this.processName = processName;
        }

        // Helper method to check if the block is available
        public boolean isFree() {
            return processName == null;
        }
    }

    // Global variables to track memory state and simulation statistics
    static List<MemoryBlock> memory = new ArrayList<>();
    static int totalMemory = 0;
    static int successfulAllocations = 0;
    static int failedAllocations = 0;

    // Runs the simulation
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide an input file. Usage: java MemoryAllocationLab <filename>");
            return;
        }
        
        System.out.println("========================================");
        System.out.println("Memory Allocation Simulator (First-Fit)");
        System.out.println("========================================");
        
        processRequests(args[0]);
        displayStatistics();
    }

    // Reads the input file and processes memory allocation and deallocation requests
    private static void processRequests(String filename) {
        System.out.println("\nReading from: " + filename);
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            // The first line of the file contains the total memory capacity
            totalMemory = Integer.parseInt(br.readLine().trim());
            System.out.println("Total Memory: " + totalMemory + " KB");
            System.out.println("----------------------------------------\n");
            
            // Initialize the memory list with a single block representing all available space
            memory.add(new MemoryBlock(0, totalMemory, null));
            
            System.out.println("Processing requests...\n");
            
            String line;
            // Read and parse each subsequent line for commands
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.trim().split("\\s+");
                String command = parts[0];
                
                // Route to the appropriate helper method based on the command
                if (command.equals("REQUEST")) {
                    String processName = parts[1];
                    int size = Integer.parseInt(parts[2]);
                    allocate(processName, size);
                } else if (command.equals("RELEASE")) {
                    String processName = parts[1];
                    deallocate(processName);
                }
            }
            
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Error parsing memory size in file.");
        }
    }

    // Implements the First-Fit allocation algorithm
    private static void allocate(String processName, int size) {
        // Scan memory from the beginning for the first suitable free block
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);
            
            // A suitable block must be free and large enough to hold the process
            if (block.isFree() && block.size >= size) {
                
                // If the block is strictly larger than requested, it must be split
                if (block.size > size) {
                    int remainingSize = block.size - size;
                    int newStart = block.start + size;
                    
                    // Create a new block for the leftover free space
                    MemoryBlock freeBlock = new MemoryBlock(newStart, remainingSize, null);
                    
                    // Insert the new free block immediately after the current block
                    memory.add(i + 1, freeBlock);
                    
                    // Shrink the current block to exactly the requested size
                    block.size = size;
                }
                
                // Assign the process to the current block
                block.processName = processName;
                successfulAllocations++;
                System.out.println("REQUEST " + processName + " " + size + " KB → SUCCESS");
                return; // Exit method once allocation is successful
            }
        }
        
        // If the loop completes without returning, no block was large enough
        failedAllocations++;
        System.out.println("REQUEST " + processName + " " + size + " KB → FAILED (Insufficient Memory)");
    }

    // Frees the memory block currently held by the specified process
    private static void deallocate(String processName) {
        // Locate the block allocated to the given process name
        for (MemoryBlock block : memory) {
            if (!block.isFree() && block.processName.equals(processName)) {
                block.processName = null; // Free the block
                System.out.println("RELEASE " + processName + " → SUCCESS");
                
                // Attempt to merge the newly freed block with any adjacent free blocks
                mergeAdjacentBlocks();
                return;
            }
        }
        // If the process was not found in any allocated block
        System.out.println("RELEASE " + processName + " → FAILED (Process Not Found)");
    }

    // Scans the memory list and combines adjacent free blocks to reduce fragmentation
    private static void mergeAdjacentBlocks() {
        for (int i = 0; i < memory.size() - 1; i++) {
            MemoryBlock current = memory.get(i);
            MemoryBlock next = memory.get(i + 1);
            
            // If both the current and the next block are free, merge them into one
            if (current.isFree() && next.isFree()) {
                current.size += next.size;
                memory.remove(i + 1);
                
                // Decrement index to re-evaluate the newly merged block with the next block in line
                i--; 
            }
        }
    }

    // Displays the final state of memory blocks and summary statistics
    private static void displayStatistics() {
        System.out.println("\n========================================");
        System.out.println("Final Memory State");
        System.out.println("========================================");
        
        int allocatedMemory = 0;
        int freeMemory = 0;
        int processCount = 0;
        int freeBlockCount = 0;
        int largestFreeBlock = 0;
        
        // Print the details of every block in the memory list
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);
            int end = block.start + block.size - 1;
            
            System.out.printf("Block %d: [%d-%d] ", (i + 1), block.start, end);
            
            if (block.isFree()) {
                System.out.printf("  FREE (%d KB)\n", block.size);
                freeMemory += block.size;
                freeBlockCount++;
                if (block.size > largestFreeBlock) {
                    largestFreeBlock = block.size;
                }
            } else {
                System.out.printf("  %s (%d KB) - ALLOCATED\n", block.processName, block.size);
                allocatedMemory += block.size;
                processCount++;
            }
        }
        
        System.out.println("\n========================================");
        System.out.println("Memory Statistics");
        System.out.println("========================================");
        
        double allocatedPct = (allocatedMemory * 100.0) / totalMemory;
        double freePct = (freeMemory * 100.0) / totalMemory;
        
        // External fragmentation calculation (total free space minus the largest available block)
        double externalFragPct = 0;
        if (freeMemory > 0) {
            externalFragPct = ((freeMemory - largestFreeBlock) * 100.0) / totalMemory;
        }
        
        System.out.printf("Total Memory:           %d KB\n", totalMemory);
        System.out.printf("Allocated Memory:       %d KB (%.2f%%)\n", allocatedMemory, allocatedPct);
        System.out.printf("Free Memory:            %d KB (%.2f%%)\n", freeMemory, freePct);
        System.out.printf("Number of Processes:    %d\n", processCount);
        System.out.printf("Number of Free Blocks:  %d\n", freeBlockCount);
        System.out.printf("Largest Free Block:     %d KB\n", largestFreeBlock);
        System.out.printf("External Fragmentation: %.2f%%\n\n", externalFragPct);
        System.out.printf("Successful Allocations: %d\n", successfulAllocations);
        System.out.printf("Failed Allocations:     %d\n", failedAllocations);
        System.out.println("========================================");
    }
}
