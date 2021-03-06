package org.mskcc.cbio.oncokb.api.pvt;

import com.mysql.jdbc.StringUtils;
import io.swagger.annotations.ApiParam;
import org.mskcc.cbio.oncokb.apiModels.*;
import org.mskcc.cbio.oncokb.apiModels.download.DownloadAvailability;
import org.mskcc.cbio.oncokb.apiModels.download.FileExtension;
import org.mskcc.cbio.oncokb.apiModels.download.FileName;
import org.mskcc.cbio.oncokb.bo.AlterationBo;
import org.mskcc.cbio.oncokb.bo.PortalAlterationBo;
import org.mskcc.cbio.oncokb.model.*;
import org.mskcc.cbio.oncokb.model.TumorType;
import org.mskcc.cbio.oncokb.bo.OncokbTranscriptService;
import org.mskcc.cbio.oncokb.util.*;
import org.oncokb.oncokb_transcript.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.mskcc.cbio.oncokb.util.GitHubUtils.getOncoKBSqlDumpFileName;
import static org.mskcc.cbio.oncokb.util.HttpUtils.getDataDownloadResponseEntity;

/**
 * Created by Hongxin on 10/28/16.
 */
@Controller
public class PrivateUtilsApiController implements PrivateUtilsApi {

    @Override
    public ResponseEntity<List<String>> utilsSuggestedVariantsGet() {
        HttpStatus status = HttpStatus.OK;

        List<String> variants = new ArrayList<>(AlterationUtils.getGeneralVariants());

        return new ResponseEntity<>(variants, status);
    }

    @Override
    public ResponseEntity<Boolean> utilsHotspotMutationGet(
        @ApiParam(value = "Gene hugo symbol") @RequestParam(value = "hugoSymbol") String hugoSymbol
        , @ApiParam(value = "Variant name") @RequestParam(value = "variant") String variant
    ) {
        HttpStatus status = HttpStatus.OK;

        Boolean isHotspot = false;

        Alteration alteration = AlterationUtils.getAlteration(hugoSymbol, variant, null, null, null, null, null);

        if (alteration != null) {
            isHotspot = HotspotUtils.isHotspot(alteration);
        }

        return new ResponseEntity<>(isHotspot, status);
    }

    @Override
    public ResponseEntity<GeneNumber> utilsNumbersGeneGet(
        @ApiParam(value = "The gene symbol used in Human Genome Organisation.", required = true) @PathVariable("hugoSymbol") String hugoSymbol
    ) {
        HttpStatus status = HttpStatus.OK;
        Set<GeneNumber> geneNumbers = NumberUtils.getGeneNumberListWithLevels(Collections.singleton(GeneUtils.getGeneByHugoSymbol(hugoSymbol)), LevelUtils.getPublicLevels());
        GeneNumber geneNumber = null;

        if (geneNumbers.size() == 1) {
            geneNumber = geneNumbers.iterator().next();
        } else {
            status = HttpStatus.NO_CONTENT;
        }

        return new ResponseEntity<>(geneNumber, status);
    }

    @Override
    public ResponseEntity<Set<GeneNumber>> utilsNumbersGenesGet() {

        Set<GeneNumber> genes = new HashSet<>();

        if (CacheUtils.getNumbers("genes") == null) {
            genes = NumberUtils.getAllGeneNumberListByLevels(LevelUtils.getPublicLevels());
            CacheUtils.setNumbers("genes", genes);
        } else {
            genes = (Set<GeneNumber>) CacheUtils.getNumbers("genes");
        }
        return new ResponseEntity<>(genes, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MainNumber> utilsNumbersMainGet() {
        MainNumber mainNumber = new MainNumber();


        if (CacheUtils.getNumbers("main") == null) {
            Set<Gene> allGenes = CacheUtils.getAllGenes();
            Integer numRealGenes = 0;
            for (Gene gene : allGenes) {
                if (gene.getEntrezGeneId() > 0)
                    numRealGenes++;
            }
            mainNumber.setGene(numRealGenes);

            List<Alteration> alterations = new ArrayList<>(AlterationUtils.getAllAlterations());
            alterations = AlterationUtils.excludeVUS(alterations);
            alterations = AlterationUtils.excludeInferredAlterations(alterations);

            mainNumber.setAlteration(alterations.size());
            Set<Evidence> evidences = CacheUtils.getAllEvidences();
            Set<TumorType> treatmentTumorTypes = new HashSet<>();
            for (Evidence evidence : evidences) {
                if (evidence.getLevelOfEvidence() != null && !evidence.getCancerTypes().isEmpty()) {
                    treatmentTumorTypes.addAll(evidence.getCancerTypes());
                }
            }
            mainNumber.setTumorType(treatmentTumorTypes.size());
            mainNumber.setDrug(NumberUtils.getDrugsCountByLevels(LevelUtils.getPublicLevels()));
            CacheUtils.setNumbers("main", mainNumber);
        } else {
            mainNumber = (MainNumber) CacheUtils.getNumbers("main");
        }

        return new ResponseEntity<>(mainNumber, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Set<LevelNumber>> utilsNumbersLevelsGet() {
        Set<LevelNumber> genes = new HashSet<>();


        if (CacheUtils.getNumbers("levels") == null) {
            genes = NumberUtils.getLevelNumberListByLevels(LevelUtils.getPublicLevels());
            CacheUtils.setNumbers("levels", genes);
        } else {
            genes = (Set<LevelNumber>) CacheUtils.getNumbers("levels");
        }

        return new ResponseEntity<>(genes, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Map<String, Boolean>> validateTrials(@ApiParam(value = "NCTID list") @RequestParam(value = "nctIds") List<String> nctIds) throws ParserConfigurationException, SAXException, IOException {
        return new ResponseEntity<>(MainUtils.validateTrials(nctIds), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<MatchVariantResult>> validateVariantExamplePost(@ApiParam(value = "List of queries. Please see swagger.json for request body format.", required = true) @RequestBody(required = true) MatchVariantRequest body) {
        List<MatchVariantResult> results = new ArrayList<>();

        for (Query query : body.getQueries()) {
            MatchVariantResult matchVariantResult = new MatchVariantResult();
            matchVariantResult.setQuery(query);
            Set<MatchVariant> match = new HashSet<>();

            for (MatchVariant matchVariantRequestVariant : body.getOncokbVariants()) {
                if (query.getHugoSymbol().equals(matchVariantRequestVariant.getHugoSymbol())) {
                    boolean isMatch = matchVariant(query.getHugoSymbol(), query.getReferenceGenome(), matchVariantRequestVariant.getAlteration(), query.getAlteration());
                    if (isMatch) {
                        match.add(matchVariantRequestVariant);
                    }
                }
            }
            matchVariantResult.setResult(match);
            results.add(matchVariantResult);
        }
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<TumorType>> utilsTumorTypesGet() {
        return new ResponseEntity<>(TumorTypeUtils.getAllTumorTypes(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Map<String, Boolean>> validateVariantExampleGet(String hugoSymbol, String referenceGenome, String variant, String examples) throws ParserConfigurationException, SAXException, IOException {
        Map<String, Boolean> validation = new HashMap<>();
        ReferenceGenome matchedRG = null;
        if (!org.apache.commons.lang3.StringUtils.isEmpty(referenceGenome)) {
            matchedRG = MainUtils.searchEnum(ReferenceGenome.class, referenceGenome);
            if (matchedRG == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        for (String example : examples.split(",")) {
            validation.put(example, matchVariant(hugoSymbol, matchedRG, variant, example));
        }
        return new ResponseEntity<>(validation, HttpStatus.OK);
    }

    private boolean matchVariant(String hugoSymbol, ReferenceGenome referenceGenome, String variant, String example) {
        Gene gene = GeneUtils.getGeneByHugoSymbol(hugoSymbol);
        boolean isMatched = false;
        if (gene != null) {
            AlterationBo alterationBo = ApplicationContextSingleton.getAlterationBo();
            boolean isGeneralAlteration = AlterationUtils.isGeneralAlterations(variant);

            Alteration oncokbVariant = new Alteration();
            oncokbVariant.setGene(gene);
            oncokbVariant.setAlteration(variant);
            oncokbVariant.setName(variant);
            // Annotate OncoKB variant
            AlterationUtils.annotateAlteration(oncokbVariant, variant);

            example = example.trim();
            Alteration exampleVariant = new Alteration();
            exampleVariant.setGene(gene);
            exampleVariant.setAlteration(example);
            exampleVariant.setName(example);
            AlterationUtils.annotateAlteration(exampleVariant, example);

            LinkedHashSet<Alteration> relevantAlterations = new LinkedHashSet<>();
            if (isGeneralAlteration) {
                Set<Alteration> allAlterations = AlterationUtils.getAllAlterations(referenceGenome, gene);

                // If the general alteration is not annotated system, at least we need to add
                // it into the list for mapping.
                Alteration exactMatch = AlterationUtils.findAlteration(gene, referenceGenome, variant);
                if (exactMatch == null) {
                    allAlterations = new HashSet<>(allAlterations);
                    allAlterations.add(oncokbVariant);
                }
                relevantAlterations = alterationBo.findRelevantAlterations(referenceGenome, exampleVariant, allAlterations, true);
                for (Alteration alteration : relevantAlterations) {
                    if (alteration.getAlteration().toLowerCase().equals(variant.toLowerCase())) {
                        isMatched = true;
                        break;
                    }
                }
            } else {
                relevantAlterations = alterationBo.findRelevantAlterations(referenceGenome, exampleVariant, Collections.singleton(oncokbVariant), false);


                // We should not do alternative allele rule in here
                List<Alteration> alternativeAlleles = AlterationUtils.getAlleleAlterations(referenceGenome, exampleVariant, Collections.singleton(oncokbVariant));
                relevantAlterations.removeAll(alternativeAlleles);

                isMatched = relevantAlterations.size() > 0;
            }
        }
        return isMatched;
    }

    @Override
    public ResponseEntity<Map<LevelOfEvidence, Set<Evidence>>> utilsEvidencesByLevelsGet() {
        Map<Gene, Set<Evidence>> evidences = EvidenceUtils.getAllGeneBasedEvidences();

        Map<LevelOfEvidence, Set<Evidence>> result = new HashMap<>();

        for (Map.Entry<Gene, Set<Evidence>> entry : evidences.entrySet()) {
            for (Evidence evidence : entry.getValue()) {
                LevelOfEvidence level = evidence.getLevelOfEvidence();
                if (level != null && LevelUtils.getPublicLevels().contains(level)) {
                    if (!result.containsKey(level)) {
                        result.put(level, new HashSet<Evidence>());
                    }
                    result.get(level).add(evidence);
                }
            }
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<TumorType>> utilRelevantTumorTypesGet(
        @ApiParam(value = "OncoTree tumor type name/main type/code") @RequestParam(value = "tumorType") String tumorType
    ) {
        return new ResponseEntity<>(TumorTypeUtils.findRelevantTumorTypes(tumorType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<TumorType>> utilRelevantCancerTypesPost(
        @ApiParam(value = "Level of Evidence") @RequestParam(value = "levelOfEvidence", required = false) LevelOfEvidence levelOfEvidence,
        @ApiParam(value = "Return only Detailed Cancer Type.") @RequestParam(value = "onlyDetailedCancerType", required = false) Boolean onlyDetailedCancerType,
        @ApiParam(value = "List of queries.", required = true) @RequestBody List<RelevantCancerTypeQuery> body
    ) {
        RelevantTumorTypeDirection direction = levelOfEvidence != null && levelOfEvidence.equals(LevelOfEvidence.LEVEL_Dx1) ? RelevantTumorTypeDirection.UPWARD : RelevantTumorTypeDirection.DOWNWARD;
        Set<TumorType> tumorTypes = body.stream()
            .map(relevantCancerTypeQuery -> StringUtils.isNullOrEmpty(relevantCancerTypeQuery.getCode()) ? TumorTypeUtils.findRelevantTumorTypes(relevantCancerTypeQuery.getMainType(), direction) : TumorTypeUtils.findRelevantTumorTypes(relevantCancerTypeQuery.getCode(), direction))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        if (onlyDetailedCancerType) {
            tumorTypes = tumorTypes.stream().filter(tumorType -> tumorType.getLevel() > 0).collect(Collectors.toSet());
        }
        return new ResponseEntity<>(
            new ArrayList<>(tumorTypes)
            , HttpStatus.OK
        );
    }

    @Override
    public ResponseEntity<VariantAnnotation> utilVariantAnnotationGet(
        @ApiParam(value = "hugoSymbol") @RequestParam(value = "hugoSymbol", required = false) String hugoSymbol
        , @ApiParam(value = "entrezGeneId") @RequestParam(value = "entrezGeneId", required = false) Integer entrezGeneId
        , @ApiParam(value = "Reference genome, either GRCh37 or GRCh38. The default is GRCh37", defaultValue = "GRCh37") @RequestParam(value = "referenceGenome", required = false, defaultValue = "GRCh37") String referenceGenome
        , @ApiParam(value = "Alteration") @RequestParam(value = "alteration", required = false) String alteration
        , @ApiParam(value = "HGVS genomic format. Example: 7:g.140453136A>T") @RequestParam(value = "hgvsg", required = false) String hgvsg
        , @ApiParam(value = "OncoTree tumor type name/main type/code") @RequestParam(value = "tumorType", required = false) String tumorType) {



        List<TumorType> relevantTumorTypes = TumorTypeUtils.findRelevantTumorTypes(tumorType);

        Query query;
        Gene gene;
        ReferenceGenome matchedRG = null;
        if (!org.apache.commons.lang3.StringUtils.isEmpty(referenceGenome)) {
            matchedRG = MainUtils.searchEnum(ReferenceGenome.class, referenceGenome);
            if (matchedRG == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        if (StringUtils.isNullOrEmpty(hgvsg)) {
            gene = GeneUtils.getGene(entrezGeneId, hugoSymbol);
            Alteration alterationModel = AlterationUtils.findAlteration(gene, matchedRG, alteration);
            if (alterationModel == null) {
                alterationModel = AlterationUtils.getAlteration(gene.getHugoSymbol(), alteration, null, null, null, null, matchedRG);
            }
            query = new Query(alterationModel, matchedRG);
        } else {
            query = new Query(null, matchedRG, "regular", null, null, null, null, null, tumorType, null, null, null, hgvsg);
            gene = GeneUtils.getGeneByEntrezId(query.getEntrezGeneId());
        }
        query.setTumorType(tumorType);

        List<EvidenceQueryRes> responses = EvidenceUtils.processRequest(Collections.singletonList(query), new HashSet<>(EvidenceTypeUtils.getAllEvidenceTypes()),LevelUtils.getPublicLevels(), false);
        IndicatorQueryResp indicatorQueryResp = IndicatorUtils.processQuery(query,null, false, null);

        EvidenceQueryRes response = responses.iterator().next();

        VariantAnnotation annotation = new VariantAnnotation(indicatorQueryResp);

        Set<Evidence> background = EvidenceUtils.getEvidenceByGeneAndEvidenceTypes(gene, Collections.singleton(EvidenceType.GENE_BACKGROUND));
        if (background.size() > 0) {
            annotation.setBackground(background.iterator().next().getDescription());
        }

        for (TumorType uniqueTumorType : response.getEvidences().stream().filter(evidence -> !evidence.getCancerTypes().isEmpty()).map(evidence -> evidence.getCancerTypes()).flatMap(Collection::stream).collect(Collectors.toSet())) {
            VariantAnnotationTumorType variantAnnotationTumorType = new VariantAnnotationTumorType();
            variantAnnotationTumorType.setRelevantTumorType(relevantTumorTypes.contains(uniqueTumorType));
            variantAnnotationTumorType.setTumorType(uniqueTumorType);
            variantAnnotationTumorType.setEvidences(response.getEvidences().stream().filter(evidence -> !evidence.getCancerTypes().isEmpty() && evidence.getCancerTypes().contains(uniqueTumorType)).collect(Collectors.toList()));
            annotation.getTumorTypes().add(variantAnnotationTumorType);
        }
        return new ResponseEntity<>(annotation, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<CancerTypeCount>> utilPortalAlterationSampleCountGet(
        @ApiParam(value = "hugoSymbol") @RequestParam(value = "hugoSymbol", required = false) String hugoSymbol
    ) {
        PortalAlterationBo portalAlterationBo = ApplicationContextSingleton.getPortalAlterationBo();
        List<CancerTypeCount> counts = new ArrayList<>();
        if (hugoSymbol == null) {
            counts.addAll(portalAlterationBo.findPortalAlterationCount());
        } else {
            Gene gene = GeneUtils.getGeneByHugoSymbol(hugoSymbol);
            counts.addAll(portalAlterationBo.findPortalAlterationCountByGene(gene));

        }
        return new ResponseEntity<>(counts, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<PortalAlteration>> utilMutationMapperDataGet(
        @ApiParam(value = "hugoSymbol") @RequestParam(value = "hugoSymbol", required = false) String hugoSymbol
    ) {
        PortalAlterationBo portalAlterationBo = ApplicationContextSingleton.getPortalAlterationBo();
        List<PortalAlteration> portalAlterations = new ArrayList<>();
        Gene gene = GeneUtils.getGeneByHugoSymbol(hugoSymbol);
        portalAlterations.addAll(portalAlterationBo.findMutationMapperData(gene));
        return new ResponseEntity<>(portalAlterations, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> utilUpdateTranscriptGet(
        @ApiParam(value = "hugoSymbol") @RequestParam(required = false) String hugoSymbol
        , @ApiParam(value = "entrezGeneId") @RequestParam(required = false) Integer entrezGeneId
        , @ApiParam(value = "grch37Isoform") @RequestParam String grch37Isoform
        , @ApiParam(value = "grch37RefSeq") @RequestParam String grch37RefSeq
        , @ApiParam(value = "grch38Isoform") @RequestParam String grch38Isoform
        , @ApiParam(value = "grch38RefSeq") @RequestParam String grch38RefSeq
    ) throws ApiException {
        // this is an util to upgrade oncokb transcript which operates on the grch37
        Gene gene = GeneUtils.getGene(entrezGeneId, hugoSymbol);

        if (gene == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        OncokbTranscriptService oncokbTranscriptService = new OncokbTranscriptService();
        oncokbTranscriptService.updateTranscriptUsage(
            gene,
            grch37Isoform,
            grch38Isoform
        );

        gene.setGrch37Isoform(grch37Isoform);
        gene.setGrch37RefSeq(grch37RefSeq);
        gene.setGrch38Isoform(grch38Isoform);
        gene.setGrch38RefSeq(grch38RefSeq);

        ApplicationContextSingleton.getGeneBo().update(gene);
        CacheUtils.updateGene(Collections.singleton(gene.getEntrezGeneId()), true);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<DownloadAvailability>> utilDataAvailabilityGet() {
        return new ResponseEntity<>(CacheUtils.getDownloadAvailabilities(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> utilDataReadmeGet(
        @ApiParam(value = "version") @RequestParam(value = "version", required = false) String version
    ) {
        return getDataDownloadResponseEntity(version, FileName.README, FileExtension.MARK_DOWN);
    }

    @Override
    public ResponseEntity<byte[]> utilDataSqlDumpGet(
        @ApiParam(value = "version") @RequestParam(value = "version", required = false) String version
    ) {
        return getDataDownloadResponseEntity(version, getOncoKBSqlDumpFileName(version), FileExtension.GZ);
    }
}
