package io.elastest.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import io.elastest.model.Trace;

public interface TraceRepository extends JpaRepository<Trace, Long> {

}
