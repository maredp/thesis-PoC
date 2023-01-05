package me.dupreez.thesisPoC.api;

import me.dupreez.thesisPoC.redesign.domain.*;
import me.dupreez.thesisPoC.redesign.service.InputParserForDecompositionAndFunctionalities;
import me.dupreez.thesisPoC.redesign.service.InputParserForRedesign;
import me.dupreez.thesisPoC.redesign.service.RedesignService;
import me.dupreez.thesisPoC.redesign.utils.Constants;
import me.dupreez.thesisPoC.redesign.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/api/redesign/codebase/{codebaseName}/decomposition/{decompositionName}")
public class RedesignController {

    private static Logger logger = LoggerFactory.getLogger(RedesignController.class);

    private static Set<Codebase> codebases = new HashSet<>();

    @RequestMapping(value = "/functionality/{functionalityName}/addRedesign/{redesignName}", method = RequestMethod.POST)
    public ResponseEntity<Decomposition> addRedesign(
            @PathVariable String codebaseName,
            @PathVariable String decompositionName,
            @PathVariable String functionalityName,
            @PathVariable String redesignName,
            @RequestBody HashMap<String, Object> data
    ) {

        logger.debug(String.format("addRedesign - %s", functionalityName));
        long startTime = System.nanoTime();

        try {
            Decomposition decomposition = initDecompositionForCodebase(codebaseName, decompositionName);
            Functionality functionality = decomposition.getFunctionality(functionalityName);
            InputParserForRedesign inputParser = new InputParserForRedesign();
            String filePath = (String) data.get("filePath");
            List<FunctionalityRedesign> redesigns = inputParser.parseInput(decomposition, functionality, redesignName, filePath);
            functionality.getFunctionalityRedesigns().addAll(redesigns);

            boolean doRedesign = data.get("doRedesign") != null && (Integer) data.get("doRedesign") == 1;
            if (doRedesign) {
                RedesignService redesignService = initRedesignService(decomposition, functionality, data);
                redesign(decomposition, redesignService, redesigns.get(0), data, filePath+"/"+decompositionName);
            } else {
                decomposition.setFunctionalityToShow(functionality);
                Utils.storeFunctionalityToShowAsJsonFile(filePath+"/"+decompositionName+"/", decomposition, 0);
            }

            long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            logger.debug(String.format("DONE - took %s seconds", seconds));


            return new ResponseEntity<>(decomposition, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/functionality/{functionalityName}/redesignFunctionality", method = RequestMethod.POST)
    public ResponseEntity<Decomposition> redesignFunctionality(
            @PathVariable String codebaseName,
            @PathVariable String decompositionName,
            @PathVariable String functionalityName,
            @RequestBody HashMap<String, Object> data
    ) {

        logger.debug(String.format("redesignFunctionality - %s", functionalityName));
        long startTime = System.nanoTime();

        try {
            Decomposition decomposition = initDecompositionForCodebase(codebaseName, decompositionName);
            Functionality functionality = decomposition.getFunctionality(functionalityName);
            RedesignService redesignService = initRedesignService(decomposition, functionality, data);

            redesign(decomposition, redesignService, null, data, Constants.RESULTS_PATH+codebaseName+"/"+decompositionName);

            long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            logger.debug(String.format("DONE - took %s seconds", seconds));

            return new ResponseEntity<>(decomposition, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private RedesignService initRedesignService(
            Decomposition decomposition,
            Functionality functionality,
            HashMap<String, Object> data
    ) throws Exception {
        if (data.get("funcComplexityWeight")!=null &&
                data.get("systemComplexityWeight")!=null &&
                data.get("queryComplexityWeight")!=null &&
                data.get("invocationsWeight")!=null
        ) {
            return new RedesignService(
                    functionality,
                    (float) data.get("funcComplexityWeight"),
                    (float) data.get("systemComplexityWeight"),
                    (float) data.get("queryComplexityWeight"),
                    (float) data.get("invocationsWeight")
            );
        }
        return new RedesignService(decomposition, functionality);
    }

    private Decomposition initDecompositionForCodebase(String codebaseName, String decompositionName) throws Exception {
        Codebase codebase = codebases.stream()
                .filter(c->c.getName().equals(codebaseName))
                .findFirst()
                .orElse(new Codebase(codebaseName));
        codebases.add(codebase);

        InputParserForDecompositionAndFunctionalities inputParser = new InputParserForDecompositionAndFunctionalities();
        return inputParser.parseInput(codebase, decompositionName);
    }

    private void redesign(
            Decomposition decomposition,
            RedesignService redesignService,
            FunctionalityRedesign initialRedesign,
            HashMap<String, Object> data,
            String filePath
    )
            throws Exception
    {
        boolean applyOperations = data.get("applyOperations") != null && (Integer) data.get("applyOperations") == 1;
        boolean characterizeLTs =  data.get("characterizeLTs") != null && (Integer) data.get("characterizeLTs") == 1;
        boolean orchestration = data.get("orchestration") != null && (Integer) data.get("orchestration") == 1;
        boolean hashing = data.get("hashing") != null && (Integer) data.get("hashing") == 1;
        boolean saveSolutionSet = data.get("saveSolutionSet") != null && (Integer) data.get("saveSolutionSet") == 1;
        int limit = data.get("limit") != null ? (Integer) data.get("limit") : 0;
        redesignService.resetFunctionality(initialRedesign);
        redesignService.doRedesign(applyOperations, characterizeLTs, orchestration, hashing, saveSolutionSet, limit, initialRedesign);

        decomposition.setFunctionalityToShow(redesignService.getFunctionality());
        Utils.storeFunctionalityToShowAsJsonFile(filePath+"/", decomposition, limit);
    }

}
