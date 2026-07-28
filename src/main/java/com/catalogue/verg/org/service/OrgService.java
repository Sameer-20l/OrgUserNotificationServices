package com.catalogue.verg.org.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.catalogue.verg.core.dto.CustomResponse;
import com.catalogue.verg.core.elasticsearch.dto.SearchCriteria;
import org.springframework.web.multipart.MultipartFile;


public interface OrgService {

    CustomResponse createOrg(JsonNode orgEntity);

    CustomResponse searchOrg(SearchCriteria searchCriteria);

    CustomResponse assignOrg(JsonNode orgEntity, String token);

    CustomResponse read(String id);

    CustomResponse delete(String id);

    CustomResponse importData(MultipartFile file);
}