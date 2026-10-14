package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.UpdatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.ValidatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;

import java.util.UUID;

public interface PcCaseService {

    PcCaseResponse createPcCase(CreatePcCaseRequest request);

    PcCaseResponse updatePcCase(UUID id, UpdatePcCaseRequest request);

    CompatibilityResult validatePcCase(ValidatePcCaseRequest request);
}
