package ru.practicum.controller.compilation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.service.compilation.CompilationService;

@RestController
@RequestMapping(path = "/admin/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCompilationController {
    private final CompilationService compilationService;

    @PostMapping
    public ResponseEntity<CompilationDto> createCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {
        log.info("POST /admin/compilations");
        return ResponseEntity.status(HttpStatus.CREATED).body(compilationService.createCompilation(newCompilationDto));
    }

    @PatchMapping("/{compId}")
    public ResponseEntity<CompilationDto> updateCompilation(@Positive @PathVariable long compId,
                                                            @RequestBody @Valid UpdateCompilationRequest updateCompilation) {
        log.info("PATCH /admin/compilations/{}", compId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(compilationService.updateCompilation(compId, updateCompilation));
    }

    @DeleteMapping("/{compId}")
    public ResponseEntity<Void> deleteCompilation(@Positive @PathVariable long compId) {
        log.info("DELETE /admin/compilations/{}", compId);
        compilationService.deleteCompilation(compId);
        return ResponseEntity.noContent().build();
    }
}
