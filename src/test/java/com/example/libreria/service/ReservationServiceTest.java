package com.example.libreria.service;

import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private BookService bookService;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private ReservationService reservationService;
    
    private User testUser;
    private Book testBook;
    private Reservation testReservation;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");
        
        testBook = new Book();
        testBook.setExternalId(258027L);
        testBook.setTitle("The Lord of the Rings");
        testBook.setPrice(new BigDecimal("15.99"));
        testBook.setStockQuantity(10);
        testBook.setAvailableQuantity(5);
        
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setRentalDays(7);
        testReservation.setStartDate(LocalDate.now());
        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setDailyRate(new BigDecimal("15.99"));
        testReservation.setTotalFee(new BigDecimal("111.93"));
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCreateReservation_Success() {
        // TODO: Implementar el test de creación de reserva

        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setBookExternalId(258027L);
        requestDTO.setRentalDays(7);
        requestDTO.setStartDate(LocalDate.now());

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findByExternalId(258027L)).thenReturn(Optional.of(testBook));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);
        doNothing().when(bookService).decreaseAvailableQuantity(258027L);

        ReservationResponseDTO result = reservationService.createReservation(requestDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals("Juan Pérez", result.getUserName());
        assertEquals(258027L, result.getBookExternalId());
        assertEquals("The Lord of the Rings", result.getBookTitle());
        assertEquals(7, result.getRentalDays());
        assertEquals(Reservation.ReservationStatus.ACTIVE, result.getStatus());
        assertEquals(new BigDecimal("111.93"), result.getTotalFee());
        assertEquals(BigDecimal.ZERO, result.getLateFee());

        verify(userService, times(1)).getUserEntity(1L);
        verify(bookRepository, times(1)).findByExternalId(258027L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        verify(bookService, times(1)).decreaseAvailableQuantity(258027L);
    }
    
    @Test
    void testCreateReservation_BookNotAvailable() {
        // TODO: Implementar el test de creación de reserva cuando el libro no está disponible
        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setBookExternalId(258027L);
        requestDTO.setRentalDays(7);
        requestDTO.setStartDate(LocalDate.now());

        testBook.setAvailableQuantity(0);

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findByExternalId(258027L)).thenReturn(Optional.of(testBook));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reservationService.createReservation(requestDTO);
        });

        assertEquals("No hay copias disponibles del libro: The Lord of the Rings", exception.getMessage());

        verify(userService, times(1)).getUserEntity(1L);
        verify(bookRepository, times(1)).findByExternalId(258027L);
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(bookService, never()).decreaseAvailableQuantity(anyLong());
    }
    
    @Test
    void testReturnBook_OnTime() {
        // TODO: Implementar el test de devolución de libro en tiempo
        LocalDate returnDate = LocalDate.now().plusDays(5); // Devuelve antes de la fecha esperada
        ReturnBookRequestDTO returnRequest = new ReturnBookRequestDTO();
        returnRequest.setReturnDate(returnDate);

        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setLateFee(BigDecimal.ZERO);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);
        doNothing().when(bookService).increaseAvailableQuantity(258027L);

        ReservationResponseDTO result = reservationService.returnBook(1L, returnRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(returnDate, result.getActualReturnDate());
        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getLateFee());

        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        verify(bookService, times(1)).increaseAvailableQuantity(258027L);
    }
    
    @Test
    void testReturnBook_Overdue() {
        // TODO: Implementar el test de devolución de libro con retraso
        LocalDate returnDate = LocalDate.now().plusDays(10); // 3 días de retraso
        ReturnBookRequestDTO returnRequest = new ReturnBookRequestDTO();
        returnRequest.setReturnDate(returnDate);

        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);

        BigDecimal expectedLateFee = new BigDecimal("7.20");

        Reservation savedReservation = new Reservation();
        savedReservation.setId(1L);
        savedReservation.setUser(testUser);
        savedReservation.setBook(testBook);
        savedReservation.setRentalDays(7);
        savedReservation.setStartDate(testReservation.getStartDate());
        savedReservation.setExpectedReturnDate(testReservation.getExpectedReturnDate());
        savedReservation.setActualReturnDate(returnDate);
        savedReservation.setDailyRate(new BigDecimal("15.99"));
        savedReservation.setTotalFee(new BigDecimal("111.93"));
        savedReservation.setLateFee(expectedLateFee);
        savedReservation.setStatus(Reservation.ReservationStatus.OVERDUE);
        savedReservation.setCreatedAt(testReservation.getCreatedAt());

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);
        doNothing().when(bookService).increaseAvailableQuantity(258027L);

        ReservationResponseDTO result = reservationService.returnBook(1L, returnRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(returnDate, result.getActualReturnDate());
        assertEquals(Reservation.ReservationStatus.OVERDUE, result.getStatus());
        assertEquals(expectedLateFee, result.getLateFee());

        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        verify(bookService, times(1)).increaseAvailableQuantity(258027L);
    }
    
    @Test
    void testGetReservationById_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        ReservationResponseDTO result = reservationService.getReservationById(1L);
        
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
    }
//
//    @Test
//    void testGetAllReservations() {
//        Reservation reservation2 = new Reservation();
//        reservation2.setId(2L);
//
//        when(reservationRepository.findAll()).thenReturn(Arrays.asList(testReservation, reservation2));
//
//        List<ReservationResponseDTO> result = reservationService.getAllReservations();
//
//        assertNotNull(result);
//        assertEquals(2, result.size());
//    }
//
    @Test
    void testGetReservationsByUserId() {
        when(reservationRepository.findByUserId(1L)).thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getReservationsByUserId(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetActiveReservations() {
        when(reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getActiveReservations();
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}

