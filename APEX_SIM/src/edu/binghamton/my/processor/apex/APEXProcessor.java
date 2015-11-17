package edu.binghamton.my.processor.apex;

import static edu.binghamton.my.common.Constants.*;
import static edu.binghamton.my.common.Utility.display;
import static edu.binghamton.my.common.Utility.echo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import edu.binghamton.my.common.FileIO;
import edu.binghamton.my.service.SimulatorHelper;

public class APEXProcessor {

	private static Integer PC = 20000;
	private static Integer UPDATED_PC = 0;
	private static String[] lastTwoInstructions = new String[2];
	private static LinkedList<String> printQueue = new LinkedList<>();
	private static Queue<String> fetchDecodeLatch = new LinkedList<>();
	private static Map<String, Integer> REGISTER_FILE = new HashMap<>();
	private static Queue<String> decodeExecuteLatch = new LinkedList<>();
	private static Queue<String> executeMemoryLatch = new LinkedList<>();
	private static Queue<String> memoryWriteBackLatch = new LinkedList<>();
	private static List<String> instructionList = new ArrayList<String>(PC);
	private static Integer[] MEMORY_ARRAY = new Integer[10000];
	private static String lastFetchedInstruction;
	private static boolean isFetchDone, isDecodeDone, isExecuteDone, isMemoryDone, isWriteBackDone;
	private static boolean BRANCH_TAKEN = false;
	private static boolean HALT_ALERT;
	private static boolean ZERO_FLAG;

	public static void init(File file) {
		echo("\nSet PC to 20000");
		PC = 20000;

		instructionList = FileIO.loadFile(file, PC);

		lastTwoInstructions[0] = lastTwoInstructions[1] = "";

		echo("Initialize Memory...");
		for(int i = 0; i < MEMORY_ARRAY.length; i++)
			MEMORY_ARRAY[i] = 0;

		echo("Initialize Registers...");
		for(String regName : REGISTER_FILE.keySet())
			REGISTER_FILE.put(regName, 0);

		echo("Reset flags...");
		isFetchDone = isDecodeDone = isExecuteDone = isMemoryDone = isWriteBackDone = false;
		HALT_ALERT = false;
		BRANCH_TAKEN = false;
		ZERO_FLAG = false;
		echo("\nSimulator state intialized successfully");
	}

	public static void displaySimulationResult() {
		display(printQueue, REGISTER_FILE, MEMORY_ARRAY);
	}
	
	public static void simulate(int cycleCount) {
		int cycle = 0;
		LinkedList<String> tempList = new LinkedList<>();
		while(cycle != cycleCount) {
			//ADD HALT CONDITION FOR EXIT
			if(HALT_ALERT && isFetchDone && isDecodeDone && isExecuteDone && isMemoryDone && isWriteBackDone) {
				isFetchDone = isDecodeDone = isExecuteDone = isMemoryDone = isWriteBackDone = false;
				break;
			}

			doWriteBack();
			doMemory();
			doExecute();
			doDecode();
			doFetch();

			while(!printQueue.isEmpty())
				tempList.add(printQueue.removeLast());

			cycle++;

		}
		printQueue.addAll(tempList);

		if(HALT_ALERT) {
			displaySimulationResult();
			echo("\nSimulation ended due to HALT instruction...");
			System.exit(0);
		}
	}

	private static void doWriteBack() {
		String instruction = memoryWriteBackLatch.poll();
		if(instruction == null) {
			if(isMemoryDone) {
				isWriteBackDone = true;
				printQueue.add("Done");
			} else {
				printQueue.add("Stall");
			}
			return;
		}
		
		printQueue.add(instruction);
		if(HALT_INSTRUCTION.equalsIgnoreCase(instruction)) {
			isWriteBackDone = true;
			return;
		}
			
		String actionType = instruction.split(" ")[0];
		switch (actionType) {
			case SQUASH_INSTRUCTION:
			case STORE_INSTRUCTION:
			case BZ_INSTRUCTION:
			case BNZ_INSTRUCTION:
				//do nothing
			break;

			case WRITE_BACK:
			case LOAD_INSTRUCTION:
				String[] parts = instruction.split(" ");
				String register = parts[1];
				Integer value = Integer.parseInt(parts[2]);
				REGISTER_FILE.put(register, value);
			break;

			default:
			break;
		}
	}

	private static void doMemory() {
		String instruction = executeMemoryLatch.poll();
		if(instruction == null) {
			if(isExecuteDone) {
				isMemoryDone = true;
				printQueue.add("Done");
			} else {
				printQueue.add("Stall");
			}
			return;
		}

		if(HALT_INSTRUCTION.equalsIgnoreCase(instruction)) {
			isMemoryDone = true;
		} else {
			String actionType = instruction.split(" ")[0];
			String[] parts;
			Integer result;
			Integer index;
			switch (actionType) {
				case SQUASH_INSTRUCTION:
				case WRITE_BACK:
				case BZ_INSTRUCTION:
				case BNZ_INSTRUCTION:
					//do nothing
				break;

				case LOAD_INSTRUCTION:
					parts = instruction.split(" ");
					index = Integer.parseInt(parts[2]);
					result = MEMORY_ARRAY[index];
					instruction = actionType + " " + parts[1] + " " + result;
				break;

				case STORE_INSTRUCTION:
					parts = instruction.split(" ");
					result = Integer.parseInt(parts[1]);
					index = Integer.parseInt(parts[2]);
					MEMORY_ARRAY[index] = result;
				break;

				default:
				break;
			}
		}
		
		printQueue.add(instruction);
		memoryWriteBackLatch.add(instruction);
	}

	private static void doExecute() {
		String instruction = decodeExecuteLatch.poll();
		if(instruction == null) {
			if(isDecodeDone) {
				isExecuteDone = true;
				printQueue.add("Done");
			} else {
				printQueue.add("Stall");
			}
			return;
		} 

		if(HALT_INSTRUCTION.equalsIgnoreCase(instruction)) {
			isExecuteDone = true;
		} else {
			String actionType = instruction.split(" ")[0];
			switch (actionType) {
			case SQUASH_INSTRUCTION:
			case WRITE_BACK:
				//do nothing
				break;
			case ADD_INSTRUCTION:
			case SUB_INSTRUCTION:
			case MUL_INSTRUCTION:
			case OR_INSTRUCTION:
			case AND_INSTRUCTION:
			case EX_OR_INSTRUCTION:
				instruction = SimulatorHelper.execute3OperandArithmeticOperation(instruction, actionType);
				Integer operationResult = Integer.parseInt(instruction.split(" ")[2]);
				if(operationResult == 0) {
					ZERO_FLAG = true;
				} else {
					ZERO_FLAG = false;
				}
				break;

			case STORE_INSTRUCTION:
			case LOAD_INSTRUCTION:
				instruction = SimulatorHelper.executeLoadStoreInstruction(instruction, actionType);
				break;

			case BZ_INSTRUCTION:
				if(ZERO_FLAG) {
					Integer offSet = Integer.parseInt(instruction.split(" ")[1]);
					UPDATED_PC = (PC - 1) + offSet;
					BRANCH_TAKEN = true;
				}
				break;

			case BNZ_INSTRUCTION:
				if(!ZERO_FLAG) {
					Integer offSet = Integer.parseInt(instruction.split(" ")[1]);
					UPDATED_PC = (PC - 1) + offSet;
					BRANCH_TAKEN = true;
				}
				break;

			default:
				break;
			}
		}

		printQueue.add(instruction);
		executeMemoryLatch.add(instruction);
	}

	private static void doDecode() {
		if(isDecodeDone) {
			printQueue.add("Done");
		} else {
			String instruction = fetchDecodeLatch.poll();
			if(instruction == null) {
				if(isFetchDone)  {
					isDecodeDone = true;
					printQueue.add("Done");
				} else {
					printQueue.add("Stall");
				}
				return;
			}
			
			if(HALT_ALERT) {
				printQueue.add("Done");
			} else if(HALT_INSTRUCTION.equalsIgnoreCase(instruction)) {
				isDecodeDone = true;
				HALT_ALERT = true;
				decodeExecuteLatch.add(instruction);
				printQueue.add(instruction);
			} else {
				if(BRANCH_TAKEN) {
					instruction = SQUASH_INSTRUCTION + " " + instruction;
					lastTwoInstructions = SimulatorHelper.updateLastTwoInstructions("", lastTwoInstructions);
					printQueue.add(instruction);
				}else if(SQUASH_INSTRUCTION.equals(instruction.split(" ")[0])) {
					lastTwoInstructions = SimulatorHelper.updateLastTwoInstructions("", lastTwoInstructions);
					printQueue.add(instruction);
				}
				else {
					int stallCount = SimulatorHelper.getStallCount(instruction, lastTwoInstructions);
					if(stallCount != 0) {
						fetchDecodeLatch.add(instruction);
						printQueue.add(instruction);
						instruction = "";
					} else {
						String decodedInstruction = "";
						int category = SimulatorHelper.getInstructionCategory(instruction);
						switch (category) {
						case ONE_OPERAND_INSTRUCTION:
							//For BZ & BNZ instructions do nothing
							decodedInstruction = instruction;
							break;
							
						case TWO_OPERAND_INSTRUCTION:
							decodedInstruction = decode2OperandInstruction(instruction);
							break;
							
						case THREE_OPERAND_INSTRUCTION:
							decodedInstruction = decode3OperandInstruction(instruction);
							break;
							
						default:
							throw new RuntimeException("Unidentified expression");
						}
						printQueue.add(decodedInstruction);
						decodeExecuteLatch.add(decodedInstruction);
					}
					lastTwoInstructions = SimulatorHelper.updateLastTwoInstructions(instruction, lastTwoInstructions);
				}
			}
		}
	}

	private static void doFetch() {
		if(isFetchDone) {
			printQueue.add("Done");
		} else {
			if(!fetchDecodeLatch.isEmpty()) {
				printQueue.add(lastFetchedInstruction);
			} else if(PC == instructionList.size()) {
				isFetchDone = true;
				if(!isDecodeDone) {
					printQueue.add(lastFetchedInstruction);
				} else {
					printQueue.add("Done");
				}
			} else {
				String instruction = instructionList.get(PC++);
				fetchDecodeLatch.add(instruction);
				if(HALT_ALERT) {
					isFetchDone = true;
					String temp = fetchDecodeLatch.poll();
					instruction = SQUASH_INSTRUCTION + " " + temp;
					fetchDecodeLatch.add(instruction);
				}
				if(BRANCH_TAKEN) {
					String temp = fetchDecodeLatch.poll();
					instruction = SQUASH_INSTRUCTION + " " + temp;
					fetchDecodeLatch.add(instruction);
					PC = UPDATED_PC;
					BRANCH_TAKEN = false;
				}
				lastFetchedInstruction = instruction;
				printQueue.add(instruction);
			}
		}
	}

	private static String decode2OperandInstruction(String instruction) {
		String actionType = instruction.split(" ")[0];
		String operand1 = instruction.split(" ")[1];
		String operand2 = instruction.split(" ")[2];
		switch (actionType) {
		case MOVC_INSTRUCTION:
		case MOV_INSTRUCTION:
			instruction =  "WB " + operand1 + " " + getRegisterValueFromRF(operand2); // Ex. WB R1 6
			break;

		//Add case for BZ BNZ
		case BZ_INSTRUCTION:
		case BNZ_INSTRUCTION:
			instruction = actionType + " " + getRegisterValueFromRF(operand1) + " " + operand2; //Ex BZ 0 20
			break;

		default:
			break;
		}
		return instruction;
	}

	private static String decode3OperandInstruction(String instruction) {
		String[] parts = instruction.split(" ");
		String destReg = parts[1];
		String src1 = parts[2];
		String src2 = parts[3];
		Integer src1Value;
		Integer src2Value;

		String actionType = parts[0];
		switch (actionType) {
		case ADD_INSTRUCTION:
		case SUB_INSTRUCTION:
		case MUL_INSTRUCTION:
		case OR_INSTRUCTION:
		case AND_INSTRUCTION:
		case EX_OR_INSTRUCTION:
			src1Value = getRegisterValueFromRF(src1);
			src2Value = getRegisterValueFromRF(src2);
			instruction = actionType + " " + destReg + " " + src1Value + " " + src2Value;
			break;

		case STORE_INSTRUCTION:
			instruction = actionType + " " + getRegisterValueFromRF(destReg) + " " + getRegisterValueFromRF(src1) + " " + getRegisterValueFromRF(src2);
			break;

		case LOAD_INSTRUCTION:
			instruction = actionType + " " + destReg + " " + getRegisterValueFromRF(src1) + " " + getRegisterValueFromRF(src2);
			break;

		default:
			break;
		}
		return instruction;
	}

	private static Integer getRegisterValueFromRF(String regName) {
		if(isRegisterName(regName)) {
			return REGISTER_FILE.get(regName);
		}
		return Integer.parseInt(regName);
	}

	private static boolean isRegisterName(String regName) {
		if(regName.charAt(0) == 'R')
			return true;
		return false;
	}

}