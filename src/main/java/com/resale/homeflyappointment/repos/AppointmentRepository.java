package com.resale.homeflyappointment.repos;

import com.resale.homeflyappointment.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    Optional<List<Appointment>> findAllByCustomerIdAndStatus(Long customerId,String status);
    List<Appointment> findAllByUserIdAndStatus(Long userId,String status);
    Optional<Appointment> findTopByCustomerIdAndStartTimeIsNotNullOrderByStartTimeDesc(Long customerId);

    @Query("SELECT a FROM Appointment a WHERE a.customerId = :customerId AND a.status IN :statuses")
    Optional<List<Appointment>> findAllByCustomerIdAndStatusIn(
            @Param("customerId") Integer customerId,
            @Param("statuses") List<String> statuses);
    List<Appointment> findAllByUserId(Integer userId);
    Optional<Appointment> getAppointmentByUserIdAndIdAndStatus(Integer userId,Integer appointmentId,String status);
    Optional<Appointment> getAppointmentByUserIdAndId(Integer userId,Integer appointmentId);
    Optional<Appointment> getAppointmentByCustomerIdAndIdAndStatus(Integer userId,Integer appointmentId,String status);
    List<Appointment> findByUserId(Integer userId);
    boolean existsByUserIdAndStatus(Integer userId, String status);
    Optional<Appointment> findByUserIdAndStatusIgnoreCase(Integer userId, String status);
    Optional<Appointment> findFirstByCustomerIdAndStatus(int customerId, String status);
    boolean existsByCustomerIdAndStatus(Integer customerId, String status);
    boolean existsByUserIdAndStatusAndIdNot(Integer userId, String status, Integer id);
}

