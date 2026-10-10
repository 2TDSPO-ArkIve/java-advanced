package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.repository.UsuarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Profile("!local-nodb")
public class NotificacaoRecipientService {

	private final UsuarioRepository usuarioRepository;

	public NotificacaoRecipientService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<Usuario> responsaveisAtivosVigentes(Animal animal) {
		return usuarioRepository.findResponsaveisAtivosVigentesDoAnimal(animal.getId(), LocalDate.now());
	}
}

