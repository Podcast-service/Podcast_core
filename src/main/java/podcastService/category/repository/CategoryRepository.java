package podcastService.category.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import podcastService.category.entity.CategoryEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findAllByOrderByPositionAsc();

    Optional<CategoryEntity> findTopByOrderByPositionDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CategoryEntity c where c.id = :id")
    Optional<CategoryEntity> findByIdForUpdate(UUID id);

    @Query(value = "select pg_advisory_xact_lock(987654321)", nativeQuery = true)
    void lockOrdering();

    @Modifying
    @Query(value = "set constraints uk_categories_position deferred", nativeQuery = true)
    void deferPositionConstraint();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CategoryEntity c
           set c.position = c.position + 1
         where c.position >= :fromPosition
    """)
    void shiftRightFrom(int fromPosition);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CategoryEntity c
           set c.position = c.position + 1
         where c.position >= :fromPosition
           and c.position < :toPosition
           and c.id <> :excludedId
    """)
    void shiftRightInRange(int fromPosition, int toPosition, UUID excludedId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CategoryEntity c
           set c.position = c.position - 1
         where c.position > :fromPosition
           and c.position <= :toPosition
           and c.id <> :excludedId
    """)
    void shiftLeftInRange(int fromPosition, int toPosition, UUID excludedId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CategoryEntity c
           set c.position = c.position - 1
         where c.position > :deletedPosition
    """)
    void closeGapAfterDelete(int deletedPosition);
}
