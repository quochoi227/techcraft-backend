package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.ValidatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;

public interface PcCaseService {

    PcCaseResponse createPcCase(CreatePcCaseRequest request);

    CompatibilityResult validatePcCase(ValidatePcCaseRequest request);
}
