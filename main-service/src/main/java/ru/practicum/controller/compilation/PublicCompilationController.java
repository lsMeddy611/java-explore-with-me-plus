package ru.practicum.controller.compilation;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.service.compilation.CompilationService;

import java.util.List;

@RestController
@RequestMapping(path = "/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PublicCompilationController {
    private final CompilationService compilationService;

    @GetMapping
    public ResponseEntity<List<CompilationDto>> getCompilations(@RequestParam(defaultValue = "false") Boolean pinned,
                                                                @PositiveOrZero @RequestParam(name = "from", defaultValue = "0") Integer from,
                                                                @Positive @RequestParam(name = "size", defaultValue = "10") Integer size) {
        log.info("GET /compilations");
        return ResponseEntity.status(HttpStatus.OK).body(compilationService.getCompilations(pinned, from, size));
    }

    @GetMapping("/{compId}")
    public ResponseEntity<CompilationDto> getCompilation(@Positive @PathVariable long compId) {
        log.info("GET /compilations/{}", compId);
        return ResponseEntity.status(HttpStatus.OK).body(compilationService.getCompilation(compId));
    }
}
