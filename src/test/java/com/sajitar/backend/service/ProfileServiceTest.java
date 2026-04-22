package com.sajitar.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.repository.ProfileRepository;
import com.sajitar.backend.service.ProfileServiceConstraintFixture.ServiceConstraintSample;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Testes unitários do {@link ProfileService}: delegação ao
 * {@link ProfileRepository} e validação por anotações ({@code @Validated}).
 * Dados de exemplo vêm de {@code /fixtures/profile-service-validation.json},
 * alinhados ao padrão teste → fixture → JSON usado em {@code NameTest},
 * {@code EmailTest} e {@code LimitTest}.
 */
@SpringBootTest
@DisplayName("ProfileService")
class ProfileServiceTest {

	@MockitoBean
	private ProfileRepository repository;

	@Autowired
	private ProfileService profileService;

	@BeforeEach
	void resetRepositoryMock() {
		reset(repository);
	}

	@Nested
	@DisplayName("findById")
	class FindById {

		@Test
		@DisplayName("Delega ao repositório quando o id é válido")
		void delegatesToRepositoryWhenIdIsValid() {
			// Given
			final var id = ProfileServiceConstraintFixture.validUuid();
			when(repository.findById(id)).thenReturn(Optional.empty());

			// When
			final var result = profileService.findById(id);

			// Then
			assertThat(result).isEmpty();
			verify(repository).findById(eq(id));
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("Não chama o repositório e lança ConstraintViolationException quando id é nulo")
		void doesNotCallRepositoryWhenIdIsNull() {
			// Given
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findByIdNotNullViolation();

			// When
			final var thrown = catchThrowable(() -> profileService.findById(null));

			// Then
			verify(repository, never()).findById(any());
			verifyNoMoreInteractions(repository);
			thenSingleViolationMatchesConstraint(thrown, sample, NotNull.class);
		}
	}

	@Nested
	@DisplayName("findByEmail")
	class FindByEmail {

		@Test
		@DisplayName("Delega ao repositório quando o e-mail é válido")
		void delegatesWhenEmailValid() {
			// Given
			final var email = "user@example.com";
			when(repository.findByEmail(email)).thenReturn(Optional.empty());

			// When
			final var result = profileService.findByEmail(email);

			// Then
			assertThat(result).isEmpty();
			verify(repository).findByEmail(eq(email));
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("Violação @NotNull composta em @Email: não chama o repositório")
		void doesNotCallRepositoryWhenEmailNull() {
			// Given
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findByEmailNotNullViolation();

			// When
			final var thrown = catchThrowable(() -> profileService.findByEmail(null));

			// Then
			verify(repository, never()).findByEmail(any());
			verifyNoMoreInteractions(repository);
			thenSingleViolationMatchesConstraint(thrown, sample, NotNull.class);
		}

		@Test
		@DisplayName("Violação @Email Jakarta: não chama o repositório")
		void doesNotCallRepositoryWhenEmailMalformed() {
			// Given
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findByEmailJakartaViolation();

			// When
			final var thrown = catchThrowable(() -> profileService.findByEmail((String) sample.sampleInvalidValue()));

			// Then
			verify(repository, never()).findByEmail(any());
			verifyNoMoreInteractions(repository);
			thenSingleViolationMatchesConstraint(thrown, sample, Email.class);
		}

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.service.ProfileServiceConstraintFixture#invalidEmailFormatArguments")
		@DisplayName("E-mails inválidos (fixture): exceção e zero chamadas ao repositório")
		void rejectsInvalidEmailFromFixture(final String email, final String failureDescription) {
			// When
			final var thrown = catchThrowable(() -> profileService.findByEmail(email));

			// Then
			assertThat(thrown).as(failureDescription).isInstanceOf(ConstraintViolationException.class);
			verify(repository, never()).findByEmail(any());
		}
	}

	@Nested
	@DisplayName("findAll(limit, reverse)")
	class FindAllTwoArgs {

		@Test
		@DisplayName("reverse false chama findAllAscending")
		void callsAscendingWhenReverseFalse() {
			when(repository.findAllAscending(30)).thenReturn(Collections.emptyList());
			assertThat(profileService.findAll(30, false)).isEmpty();
			verify(repository).findAllAscending(30);
			verify(repository, never()).findAllDescending(anyInt());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("reverse true chama findAllDescending")
		void callsDescendingWhenReverseTrue() {
			when(repository.findAllDescending(30)).thenReturn(Collections.emptyList());
			assertThat(profileService.findAll(30, true)).isEmpty();
			verify(repository).findAllDescending(30);
			verify(repository, never()).findAllAscending(anyInt());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("limite nulo (@NotNull em @Limit): não chama listagens")
		void doesNotCallRepositoryWhenLimitNull() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findAllTwoArgsLimitNotNullViolation();
			final var thrown = catchThrowable(() -> profileService.findAll(null, false));
			verify(repository, never()).findAllAscending(anyInt());
			verify(repository, never()).findAllDescending(anyInt());
			thenSingleViolationMatchesConstraint(thrown, sample, NotNull.class);
		}

		@Test
		@DisplayName("limite inválido (@Limit): não chama listagens")
		void doesNotCallRepositoryWhenLimitInvalid_detailed() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findAllTwoArgsLimitPositiveViolation();
			final var thrown = catchThrowable(() -> profileService.findAll((Integer) sample.sampleInvalidValue(), false));
			verify(repository, never()).findAllAscending(anyInt());
			verify(repository, never()).findAllDescending(anyInt());
			thenSingleViolationMatchesConstraint(thrown, sample, Limit.class);
		}

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.service.ProfileServiceConstraintFixture#invalidLimitNotPositiveArguments")
		void doesNotCallRepositoryWhenLimitNotPositive(final int limit, final String failureDescription) {
			final var thrown = catchThrowable(() -> profileService.findAll(limit, false));
			assertThat(thrown).as(failureDescription).isInstanceOf(ConstraintViolationException.class);
			verify(repository, never()).findAllAscending(anyInt());
			verify(repository, never()).findAllDescending(anyInt());
		}

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.service.ProfileServiceConstraintFixture#invalidLimitExceedsMaxArguments")
		void doesNotCallRepositoryWhenLimitAboveMax(final int limit, final String failureDescription) {
			final var thrown = catchThrowable(() -> profileService.findAll(limit, false));
			assertThat(thrown).as(failureDescription).isInstanceOf(ConstraintViolationException.class);
			verify(repository, never()).findAllAscending(anyInt());
			verify(repository, never()).findAllDescending(anyInt());
		}

		@Test
		@DisplayName("reverse nulo (@NotNull): não chama listagens")
		void doesNotCallRepositoryWhenReverseNull() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findAllTwoArgsReverseNotNullViolation();
			final var thrown = catchThrowable(() -> profileService.findAll(50, null));
			verify(repository, never()).findAllAscending(anyInt());
			verify(repository, never()).findAllDescending(anyInt());
			thenSingleViolationMatchesConstraint(thrown, sample, NotNull.class);
		}
	}

	@Nested
	@DisplayName("findAll(limit, lastSeenName, lastSeenId, reverse)")
	class FindAllFourArgs {

		private final String cursorName = ProfileServiceConstraintFixture.validCursorName();
		private final UUID cursorId = ProfileServiceConstraintFixture.validUuid();

		@Test
		@DisplayName("reverse false chama findAllAscendingAfter")
		void callsAscendingAfter() {
			when(repository.findAllAscendingAfter(20, cursorName, cursorId)).thenReturn(Collections.emptyList());
			assertThat(profileService.findAll(20, cursorName, cursorId, false)).isEmpty();
			verify(repository).findAllAscendingAfter(20, cursorName, cursorId);
			verify(repository, never()).findAllDescendingAfter(anyInt(), anyString(), any());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("reverse true chama findAllDescendingAfter")
		void callsDescendingAfter() {
			when(repository.findAllDescendingAfter(20, cursorName, cursorId)).thenReturn(Collections.emptyList());
			assertThat(profileService.findAll(20, cursorName, cursorId, true)).isEmpty();
			verify(repository).findAllDescendingAfter(20, cursorName, cursorId);
			verify(repository, never()).findAllAscendingAfter(anyInt(), anyString(), any());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("lastSeenName inválido (@Name): não chama o repositório")
		void doesNotCallWhenLastSeenNameInvalid_detailed() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findAllFourArgsNamePatternViolation();
			final var thrown = catchThrowable(
					() -> profileService.findAll(50, (String) sample.sampleInvalidValue(), cursorId, false));
			verifyFindAllFourNeverCalled();
			thenSingleViolationMatchesConstraint(thrown, sample, Pattern.class);
		}

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.service.ProfileServiceConstraintFixture#invalidNamePatternArguments")
		void doesNotCallWhenLastSeenNameInvalid_fromFixture(final String badName, final String failureDescription) {
			final var thrown = catchThrowable(() -> profileService.findAll(50, badName, cursorId, false));
			assertThat(thrown).as(failureDescription).isInstanceOf(ConstraintViolationException.class);
			verifyFindAllFourNeverCalled();
		}

		@Test
		@DisplayName("lastSeenId nulo (@NotNull): não chama o repositório")
		void doesNotCallWhenLastSeenIdNull() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findAllFourArgsLastSeenIdNotNullViolation();
			final var thrown = catchThrowable(() -> profileService.findAll(50, cursorName, null, false));
			verifyFindAllFourNeverCalled();
			thenSingleViolationMatchesConstraint(thrown, sample, NotNull.class);
		}

		private void verifyFindAllFourNeverCalled() {
			verify(repository, never()).findAllAscendingAfter(anyInt(), anyString(), any());
			verify(repository, never()).findAllDescendingAfter(anyInt(), anyString(), any());
		}
	}

	@Nested
	@DisplayName("findByNameContainingIgnoreCase(limit, name, reverse)")
	class FindByNameThreeArgs {

		private final String search = ProfileServiceConstraintFixture.validSearchName();

		@Test
		@DisplayName("reverse false chama busca ascendente")
		void callsAscending() {
			when(repository.findByNameContainingIgnoreCaseAscending(10, search)).thenReturn(Collections.emptyList());
			assertThat(profileService.findByNameContainingIgnoreCase(10, search, false)).isEmpty();
			verify(repository).findByNameContainingIgnoreCaseAscending(10, search);
			verify(repository, never()).findByNameContainingIgnoreCaseDescending(anyInt(), anyString());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("reverse true chama busca descendente")
		void callsDescending() {
			when(repository.findByNameContainingIgnoreCaseDescending(10, search)).thenReturn(Collections.emptyList());
			assertThat(profileService.findByNameContainingIgnoreCase(10, search, true)).isEmpty();
			verify(repository).findByNameContainingIgnoreCaseDescending(10, search);
			verify(repository, never()).findByNameContainingIgnoreCaseAscending(anyInt(), anyString());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("limite nulo (@NotNull em @Limit): não chama o repositório")
		void doesNotCallWhenLimitNull() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findByNameThreeArgsLimitNotNullViolation();
			final var thrown = catchThrowable(
					() -> profileService.findByNameContainingIgnoreCase(null, search, false));
			verifyFindByNameThreeNeverCalled();
			thenSingleViolationMatchesConstraint(thrown, sample, NotNull.class);
		}

		@Test
		@DisplayName("name em branco (@NotBlank): não chama o repositório")
		void doesNotCallWhenNameBlank_detailed() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findByNameThreeArgsNotBlankViolation();
			final var thrown = catchThrowable(
					() -> profileService.findByNameContainingIgnoreCase(50, (String) sample.sampleInvalidValue(), false));
			verifyFindByNameThreeNeverCalled();
			thenSingleViolationMatchesConstraint(thrown, sample, NotBlank.class);
		}

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.service.ProfileServiceConstraintFixture#blankSearchNameArguments")
		void doesNotCallWhenNameBlank_fromFixture(final String blank, final String failureDescription) {
			final var thrown = catchThrowable(() -> profileService.findByNameContainingIgnoreCase(50, blank, false));
			assertThat(thrown).as(failureDescription).isInstanceOf(ConstraintViolationException.class);
			verifyFindByNameThreeNeverCalled();
		}

		private void verifyFindByNameThreeNeverCalled() {
			verify(repository, never()).findByNameContainingIgnoreCaseAscending(anyInt(), anyString());
			verify(repository, never()).findByNameContainingIgnoreCaseDescending(anyInt(), anyString());
		}
	}

	@Nested
	@DisplayName("findByNameContainingIgnoreCase(limit, lastSeenName, lastSeenId, name, reverse)")
	class FindByNameFiveArgs {

		private final String cursor = ProfileServiceConstraintFixture.validCursorName();
		private final UUID cursorId = ProfileServiceConstraintFixture.validUuid();
		private final String search = ProfileServiceConstraintFixture.validSearchName();

		@Test
		@DisplayName("reverse false chama ascending after")
		void callsAscendingAfter() {
			when(repository.findByNameContainingIgnoreCaseAscendingAfter(15, cursor, cursorId, search))
					.thenReturn(Collections.emptyList());
			assertThat(profileService.findByNameContainingIgnoreCase(15, cursor, cursorId, search, false)).isEmpty();
			verify(repository).findByNameContainingIgnoreCaseAscendingAfter(15, cursor, cursorId, search);
			verify(repository, never()).findByNameContainingIgnoreCaseDescendingAfter(anyInt(), anyString(), any(), anyString());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("reverse true chama descending after")
		void callsDescendingAfter() {
			when(repository.findByNameContainingIgnoreCaseDescendingAfter(15, cursor, cursorId, search))
					.thenReturn(Collections.emptyList());
			assertThat(profileService.findByNameContainingIgnoreCase(15, cursor, cursorId, search, true)).isEmpty();
			verify(repository).findByNameContainingIgnoreCaseDescendingAfter(15, cursor, cursorId, search);
			verify(repository, never()).findByNameContainingIgnoreCaseAscendingAfter(anyInt(), anyString(), any(), anyString());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("lastSeenName inválido (@Name): não chama o repositório")
		void doesNotCallWhenCursorNameInvalid() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.findByNameFiveArgsNamePatternViolation();
			final var thrown = catchThrowable(
					() -> profileService.findByNameContainingIgnoreCase(40, (String) sample.sampleInvalidValue(), cursorId, search, false));
			verifyFindByNameFiveNeverCalled();
			thenSingleViolationMatchesConstraint(thrown, sample, Pattern.class);
		}

		private void verifyFindByNameFiveNeverCalled() {
			verify(repository, never()).findByNameContainingIgnoreCaseAscendingAfter(anyInt(), anyString(), any(), anyString());
			verify(repository, never()).findByNameContainingIgnoreCaseDescendingAfter(anyInt(), anyString(), any(), anyString());
		}
	}

	@Nested
	@DisplayName("countAll")
	class CountAll {

		@Test
		@DisplayName("Delega a countForFindAll")
		void delegates() {
			when(repository.countForFindAll()).thenReturn(7L);
			assertThat(profileService.countAll()).isEqualTo(7L);
			verify(repository).countForFindAll();
			verifyNoMoreInteractions(repository);
		}
	}

	@Nested
	@DisplayName("countAll(lastSeenName, lastSeenId, reverse)")
	class CountAllCursored {

		private final String cursor = ProfileServiceConstraintFixture.validCursorName();
		private final UUID cursorId = ProfileServiceConstraintFixture.validUuid();

		@Test
		@DisplayName("reverse false chama countForFindAllAscendingAfter")
		void ascendingAfter() {
			when(repository.countForFindAllAscendingAfter(cursor, cursorId)).thenReturn(3L);
			assertThat(profileService.countAll(cursor, cursorId, false)).isEqualTo(3L);
			verify(repository).countForFindAllAscendingAfter(cursor, cursorId);
			verify(repository, never()).countForFindAllDescendingAfter(anyString(), any());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("reverse true chama countForFindAllDescendingAfter")
		void descendingAfter() {
			when(repository.countForFindAllDescendingAfter(cursor, cursorId)).thenReturn(2L);
			assertThat(profileService.countAll(cursor, cursorId, true)).isEqualTo(2L);
			verify(repository).countForFindAllDescendingAfter(cursor, cursorId);
			verify(repository, never()).countForFindAllAscendingAfter(anyString(), any());
			verifyNoMoreInteractions(repository);
		}
	}

	@Nested
	@DisplayName("countByNameContainingIgnoreCase(name)")
	class CountByNameOneArg {

		@Test
		@DisplayName("Delega a countForFindByNameContainingIgnoreCase")
		void delegates() {
			final var name = ProfileServiceConstraintFixture.validSearchName();
			when(repository.countForFindByNameContainingIgnoreCase(name)).thenReturn(5L);
			assertThat(profileService.countByNameContainingIgnoreCase(name)).isEqualTo(5L);
			verify(repository).countForFindByNameContainingIgnoreCase(name);
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("name vazio (@NotBlank): não chama o repositório")
		void doesNotCallWhenBlank() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.countByNameNotBlankViolation();
			final var thrown = catchThrowable(
					() -> profileService.countByNameContainingIgnoreCase((String) sample.sampleInvalidValue()));
			verify(repository, never()).countForFindByNameContainingIgnoreCase(anyString());
			thenSingleViolationMatchesConstraint(thrown, sample, NotBlank.class);
		}
	}

	@Nested
	@DisplayName("countByNameContainingIgnoreCase(lastSeenName, lastSeenId, name, reverse)")
	class CountByNameFiveArgs {

		private final String cursor = ProfileServiceConstraintFixture.validCursorName();
		private final UUID cursorId = ProfileServiceConstraintFixture.validUuid();
		private final String search = ProfileServiceConstraintFixture.validSearchName();

		@Test
		@DisplayName("reverse false chama count ascendente after")
		void ascendingAfter() {
			when(repository.countForFindByNameContainingIgnoreCaseAscendingAfter(cursor, cursorId, search)).thenReturn(4L);
			assertThat(profileService.countByNameContainingIgnoreCase(cursor, cursorId, search, false)).isEqualTo(4L);
			verify(repository).countForFindByNameContainingIgnoreCaseAscendingAfter(cursor, cursorId, search);
			verify(repository, never()).countForFindByNameContainingIgnoreCaseDescendingAfter(anyString(), any(), anyString());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("reverse true chama count descendente after")
		void descendingAfter() {
			when(repository.countForFindByNameContainingIgnoreCaseDescendingAfter(cursor, cursorId, search)).thenReturn(1L);
			assertThat(profileService.countByNameContainingIgnoreCase(cursor, cursorId, search, true)).isEqualTo(1L);
			verify(repository).countForFindByNameContainingIgnoreCaseDescendingAfter(cursor, cursorId, search);
			verify(repository, never()).countForFindByNameContainingIgnoreCaseAscendingAfter(anyString(), any(), anyString());
			verifyNoMoreInteractions(repository);
		}

		@Test
		@DisplayName("name vazio (@NotBlank) na assinatura cursored: não chama o repositório")
		void doesNotCallWhenNameBlankInFiveArgCount() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.countByNameFiveArgsNameNotBlankViolation();
			final var thrown = catchThrowable(
					() -> profileService.countByNameContainingIgnoreCase(cursor, cursorId, (String) sample.sampleInvalidValue(), false));
			verify(repository, never()).countForFindByNameContainingIgnoreCaseAscendingAfter(anyString(), any(), anyString());
			verify(repository, never()).countForFindByNameContainingIgnoreCaseDescendingAfter(anyString(), any(), anyString());
			thenSingleViolationMatchesConstraint(thrown, sample, NotBlank.class);
		}

		@Test
		@DisplayName("reverse nulo (@NotNull): não chama contagens cursored")
		void doesNotCallWhenReverseNull() {
			final ServiceConstraintSample sample = ProfileServiceConstraintFixture.countByNameFiveArgsReverseNotNullViolation();
			final var thrown = catchThrowable(
					() -> profileService.countByNameContainingIgnoreCase(cursor, cursorId, search, null));
			verify(repository, never()).countForFindByNameContainingIgnoreCaseAscendingAfter(anyString(), any(), anyString());
			verify(repository, never()).countForFindByNameContainingIgnoreCaseDescendingAfter(anyString(), any(), anyString());
			thenSingleViolationMatchesConstraint(thrown, sample, NotNull.class);
		}
	}

	private static void thenSingleViolationMatchesConstraint(
			final Throwable thrown,
			final ServiceConstraintSample sample,
			final Class<?> constraintAnnotation) {
		assertThat(thrown).as(sample.failureDescriptionViolationCount()).isInstanceOf(ConstraintViolationException.class);
		final var violations = ((ConstraintViolationException) thrown).getConstraintViolations();
		assertThat(violations).as(sample.failureDescriptionViolationCount()).hasSize(1);
		final var violation = violations.iterator().next();
		assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
				.as(sample.failureDescriptionConstraintAnnotation())
				.isEqualTo(constraintAnnotation);
		assertThat(violation.getMessage())
				.as(sample.failureDescriptionMessage())
				.isEqualTo(sample.expectedMessagePtBr());
		assertThat(violation.getPropertyPath().toString())
				.as(sample.failureDescriptionPropertyPath())
				.isEqualTo(sample.expectedPropertyPath());
	}
}
