package com.yeoboya.lunch.api.v1.file.repository;

import com.yeoboya.lunch.api.v1.file.domain.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {

}
