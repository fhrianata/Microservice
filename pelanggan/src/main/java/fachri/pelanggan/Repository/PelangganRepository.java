package fachri.pelanggan.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fachri.pelanggan.model.Pelanggan;

@Repository
public interface PelangganRepository extends JpaRepository<Pelanggan, Long>{

}