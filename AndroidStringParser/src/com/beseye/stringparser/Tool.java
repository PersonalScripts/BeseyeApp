package com.beseye.stringparser;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

public class Tool {

	public static void main(String[] args) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException, SAXException {
		if(args == null || args.length == 0){
			printHelp();
			return;
		}
		
		if("-i".equals(args[0])){
			ToolImport.run(args[1]);
		}else if("-e".equals(args[0])){
			ToolExport.run(args[1], args.length > 2 ? args[2] : null);
		}else{
			printHelp();
		}
	}
	
	private static void printHelp(){
		System.out.println("commands format:");
		System.out.println("\texport: -e <project dir> <output file>");
		System.out.println("\timport: -i <input file>");
	}
}
