package org.mskcc.cbio.oncokb.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-05-10T02:49:36.208Z")
public class ClinicalVariant {

    private Alteration variant = null;
    private String cancerType = null;
    private String level = null;
    private Set<String> drug = new HashSet<String>();
    private Set<String> drugPmids = new HashSet<String>();

    
    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("variant")
    public Alteration getVariant() {
        return variant;
    }

    public void setVariant(Alteration variant) {
        this.variant = variant;
    }


    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("cancerType")
    public String getCancerType() {
        return cancerType;
    }

    public void setCancerType(String cancerType) {
        this.cancerType = cancerType;
    }


    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("level")
    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }


    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("drug")
    public Set<String> getDrug() {
        return drug;
    }

    public void setDrug(Set<String> drug) {
        this.drug = drug;
    }


    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("drugPmids")
    public Set<String> getDrugPmids() {
        return drugPmids;
    }

    public void setDrugPmids(Set<String> drugPmids) {
        this.drugPmids = drugPmids;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClinicalVariant clinicalVariant = (ClinicalVariant) o;
        return Objects.equals(variant, clinicalVariant.variant) &&
            Objects.equals(cancerType, clinicalVariant.cancerType) &&
            Objects.equals(level, clinicalVariant.level) &&
            Objects.equals(drug, clinicalVariant.drug) &&
            Objects.equals(drugPmids, clinicalVariant.drugPmids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, cancerType, level, drug, drugPmids);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ClinicalVariant {\n");

        sb.append("  variant: ").append(variant).append("\n");
        sb.append("  cancerType: ").append(cancerType).append("\n");
        sb.append("  level: ").append(level).append("\n");
        sb.append("  drug: ").append(drug).append("\n");
        sb.append("  drugPmids: ").append(drugPmids).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
