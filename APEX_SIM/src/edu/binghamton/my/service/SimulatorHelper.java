package edu.binghamton.my.service;

import static edu.binghamton.my.common.Constants.*;

public class SimulatorHelper {

	public static String get3OperandEquivalent(String instruction) {
		if(instruction == null || "".equalsIgnoreCase(instruction)) {
			return instruction;
		}

		int category = getInstructionCategory(instruction);
		if(category == THREE_OPERAND_INSTRUCTION)
			return instruction;

		if(category == TWO_OPERAND_INSTRUCTION) {
			String[] parts = instruction.split(" ");
			return parts[0] + " " + parts[1] + " " + parts[1] + " " + parts[2];
		}
		//TODO Code for one operand

		if(category == ONE_OPERAND_INSTRUCTION) {
			String[] parts = instruction.split(" ");
			return parts[0] + " " + parts[1] + " " + parts[1] + " " + parts[1];
		}
		return null;
	}

	public static boolean executeBranchInstruction(String instruction, String actionType) {
		String[] parts = instruction.split(" ");
		Integer regValue = Integer.parseInt(parts[1]);

		if(BZ_INSTRUCTION.equalsIgnoreCase(actionType) && regValue == 0) {
			return true;
		} else if(BNZ_INSTRUCTION.equalsIgnoreCase(actionType) && regValue != 0) {
			return true;
		}
		return false;
	}

	public static String executeLoadStoreInstruction(String instruction, String actionType) {
		String[] parts = instruction.split(" ");
		Integer op2 = Integer.parseInt(parts[2]);
		Integer op3 = Integer.parseInt(parts[3]);
		Integer result = 0;
		result = op2 + op3;

		if("LOAD".equalsIgnoreCase(actionType)) {
			instruction = actionType + " " + parts[1] + " " + result;
		} else if("STORE".equalsIgnoreCase(actionType)) {
			Integer op1 = Integer.parseInt(parts[1]);
			instruction = actionType + " " + op1 + " " + result; 
		}
		return instruction;
	}

	public static String execute3OperandArithmeticOperation(String instruction, String actionType) {
		String[] parts = instruction.split(" ");
		String destReg = parts[1];
		Integer op1 = Integer.parseInt(parts[2]);
		Integer op2 = Integer.parseInt(parts[3]);
		Integer result = 0;
		switch (actionType) {
		case ADD_INSTRUCTION:
			result = op1 + op2;
			break;

		case SUB_INSTRUCTION:
			result = op1 - op2;
			break;
		case MUL_INSTRUCTION:
			result = op1 * op2;
			break;

		case AND_INSTRUCTION:
			result = op1 & op2;
			break;
		case OR_INSTRUCTION:
			result = op1 | op2;
			break;

		case EX_OR_INSTRUCTION:
			result = op1 ^ op2;
			break;

		default:
			break;
		}

		instruction = "WB " + destReg + " " + result;
		return instruction;
	}

	public static int getInstructionCategory(String instruction) {
		String actionType = instruction.split(" ")[0];

		for(String type : ONE_OPERAND_INSTRUCTIONS_ARRAY)
			if(type.equalsIgnoreCase(actionType))
				return ONE_OPERAND_INSTRUCTION;

		for(String type : TWO_OPERAND_INSTRUCTIONS_ARRAY)
			if(type.equalsIgnoreCase(actionType))
				return TWO_OPERAND_INSTRUCTION;

		for(String type : THREE_OPERAND_INSTRUCTIONS_ARRAY)
			if(type.equalsIgnoreCase(actionType))
				return THREE_OPERAND_INSTRUCTION;

		return 0;
	}

	public static int getStallCount(String instruction, String[] lastTwoInstructionArray) {
		instruction = get3OperandEquivalent(instruction);

		String[] parts = instruction.split(" ");
		String instructionType = parts[0];
		String destReg = parts[1];
		String src1 = parts[2];
		String src2 = parts[3];

		String zerothInstruction = lastTwoInstructionArray[0];
		String firstInstruction = lastTwoInstructionArray[1];

		if(zerothInstruction != null) {
			String zeroInstType = zerothInstruction.split(" ")[0];
			if(STORE_INSTRUCTION.equalsIgnoreCase(zeroInstType)) {
				//dont compare
			} else if(!"".equalsIgnoreCase(zerothInstruction) && zerothInstruction.split(" ").length > 2) {
				String temp = zerothInstruction.split(" ")[1];
				
				if(STORE_INSTRUCTION.equalsIgnoreCase(instructionType)) {
					if(temp.equalsIgnoreCase(destReg) || temp.equalsIgnoreCase(src1) || temp.equalsIgnoreCase(src2))
						return 1;
				} else {
					if(temp.equalsIgnoreCase(src1) || temp.equalsIgnoreCase(src2))
						return 1;
				}
			}
		}

		if(firstInstruction != null) {
			String firstInstType = firstInstruction.split(" ")[0];
			if("STORE".equalsIgnoreCase(firstInstType)) {
				//dont compare
			} else if(!"".equalsIgnoreCase(firstInstruction) && firstInstruction.split(" ").length > 2) {
				String temp = firstInstruction.split(" ")[1];
				
				if("STORE".equalsIgnoreCase(instructionType)) {
					if(temp.equalsIgnoreCase(destReg) || temp.equalsIgnoreCase(src1) || temp.equalsIgnoreCase(src2))
						return 2;
				} else {
					if(temp.equalsIgnoreCase(src1) || temp.equalsIgnoreCase(src2))
						return 2;
				}
			}
		}

		return 0;
	}

	public static String[] updateLastTwoInstructions(String instruction, String[] lastTwoInstructionArray) {
		lastTwoInstructionArray[0] = lastTwoInstructionArray[1];
		lastTwoInstructionArray[1] = get3OperandEquivalent(instruction);
		return lastTwoInstructionArray;
	}
}
