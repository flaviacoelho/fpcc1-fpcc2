package refdiff.reffdiff1;


import java.util.List;

import org.eclipse.jgit.lib.Repository;

import refdiff.core.RefDiff;
import refdiff.core.api.GitService;
import refdiff.core.rm2.model.refactoring.SDRefactoring;
import refdiff.core.util.GitServiceImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Random;

//import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Root;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class MainRefDiff {
	
	public static void main(String [] args) throws Exception {
		
		List<Root> roots = extractData();
		
		for(int i = 0; i < roots.size(); i++) { 
		
			RefDiff refDiff = new RefDiff();
			GitService gitService = new GitServiceImpl();
		
			String folder = "tmp/" + roots.get(i).id, project = roots.get(i).repository,
				commitId = roots.get(i).sha1;

			Path folderPath = Paths.get(folder);
			
			String filePathRefactorings = filePathComposer(folderPath, "refactorings.csv");
			String filePathExecutionTime = filePathComposer(folderPath, "executiontime.csv");
	
			Files.deleteIfExists(Paths.get(filePathRefactorings));
			Files.deleteIfExists(Paths.get(filePathExecutionTime));
			
			saveToFile(filePathRefactorings, getResultHeader());
			saveToFile(filePathExecutionTime, getExecutionTimeHeader());
		
			Repository repo = gitService.cloneIfNotExists(folder, project);
		
			try (Repository repository = gitService.cloneIfNotExists(folder, project)) {
			
				long startTime = System.nanoTime(), finishTime;
				List<SDRefactoring> refactorings = refDiff.detectAtCommit(repository, commitId);
				finishTime = System.nanoTime() - startTime;           
		            
				for (SDRefactoring r : refactorings) {
					System.out.printf("%s\t%s\t%s\n", r.getRefactoringType().getDisplayName(), r.getEntityBefore().key(), r.getEntityAfter().key());
					saveToFile(filePathRefactorings, getResultRefactoringDescription(commitId, r));
				}
				saveToFile(filePathExecutionTime, getResultExecutionTime(commitId, finishTime));
				System.out.println("Tempo em nanosegundos: " + finishTime);				
			}catch(Exception e) {
				System.err.println("Error processing commit " + commitId);
				e.printStackTrace(System.err);
			}
		}		
	}
	
	private static List<Root> extractData() throws JsonParseException, JsonMappingException, IOException{
		
		ObjectMapper mapper = new ObjectMapper();	
		
		String jsonFile = System.getProperty("user.dir") + "/data/dataSample.json";		
		
		return mapper.readValue(new File(jsonFile),
				mapper.getTypeFactory().constructCollectionType(List.class, Root.class));		
	}

	private static String filePathComposer(Path folderPath, String fileName) {		
		return folderPath.toString() + "" + fileName;				
	}
	
	//from RMiner
	private static void saveToFile(String fileName, String content) {
		Path path = Paths.get(fileName);
		byte[] contentBytes = (content + System.lineSeparator()).getBytes();
		try {
			Files.write(path, contentBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//from RMiner
	private static String getResultHeader() {
		return "CommitId;RefactoringType;RefactoringDetail";
	}
	
	private static String getExecutionTimeHeader() {
		return "CommitId;ExecutionTime";
	}
	
	//from RMiner
	private static String getResultRefactoringDescription(String commitId, SDRefactoring ref) {
		StringBuilder builder = new StringBuilder();
		builder.append(commitId);
		builder.append(";");
		builder.append(ref.getRefactoringType().getDisplayName());
		builder.append(";");
		builder.append(ref.getEntityBefore().key());		
		builder.append(";");
		builder.append(ref.getEntityAfter().key());
		return builder.toString();
	}
	
	private static String getResultExecutionTime(String commitId, long executionTime) {
		StringBuilder builder = new StringBuilder();
		builder.append(commitId);
		builder.append(";");
		builder.append(executionTime);
		return builder.toString();
	}
}

