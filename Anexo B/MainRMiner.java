import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Root;
import org.refactoringminer.util.GitServiceImpl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;



public class MainRMiner {
	
	public static void main(String [] args) throws Exception {
		
		List<Root> roots = extractData();
		
		int [] sampleIndex = generateRandomIndex(); //generating the sample
		
		refactoringDetection(roots, sampleIndex); 
				
	}
	
	private static List<Root> extractData() throws JsonParseException, JsonMappingException, IOException{
	
		ObjectMapper mapper = new ObjectMapper();	
		
		String jsonFile = System.getProperty("user.dir") + "/src-test/Data/dataSample.json";		
		
		return mapper.readValue(new File(jsonFile),
				mapper.getTypeFactory().constructCollectionType(List.class, Root.class));		
	}	
	
	
	private static int [] generateRandomIndex(){
		
		Random generator = new Random(1234567);
		int sample[] = new int[150]; 
		
		for(int i = 0; i < 150; i++) {
			sample[i] = generator.nextInt(539);			
		}
		return sample;
		
	}
	
	//from RMiner + some adaptations
	private static void refactoringDetection(List<Root> roots, int [] sampleIndex) throws Exception{
		
		for(int i = 0; i < roots.size(); i++) {
			
			GitService gitService = new GitServiceImpl();
			
			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
						
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
			
			long startTime = System.nanoTime();
			
			detector.detectAtCommit(repo, project, commitId, new RefactoringHandler() {				
				long finishTime = System.nanoTime() - startTime;
				@Override
				public void handle(RevCommit commitData, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId + " in " + finishTime + " nanosegundos");
						for (Refactoring ref : refactorings) {
							saveToFile(filePathRefactorings, getResultRefactoringDescription(commitId, ref));
						}						
						saveToFile(filePathExecutionTime, getResultExecutionTime(commitId, finishTime));
					}
				}
				
				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
			System.out.print("Finalizou");
		}
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
	private static String getResultRefactoringDescription(String commitId, Refactoring ref) {
		StringBuilder builder = new StringBuilder();
		builder.append(commitId);
		builder.append(";");
		builder.append(ref.getName());
		builder.append(";");
		builder.append(ref);		
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
