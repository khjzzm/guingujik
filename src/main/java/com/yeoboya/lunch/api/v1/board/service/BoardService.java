package com.yeoboya.lunch.api.v1.board.service;

import com.yeoboya.lunch.api.v1.board.domain.Board;
import com.yeoboya.lunch.api.v1.board.domain.BoardHashTag;
import com.yeoboya.lunch.api.v1.board.domain.HashTag;
import com.yeoboya.lunch.api.v1.board.repository.BoardRepository;
import com.yeoboya.lunch.api.v1.board.repository.HashTagRepository;
import com.yeoboya.lunch.api.v1.board.request.BoardCreate;
import com.yeoboya.lunch.api.v1.board.request.BoardSearch;
import com.yeoboya.lunch.api.v1.board.request.FileBoardCreate;
import com.yeoboya.lunch.api.v1.board.response.BoardResponse;
import com.yeoboya.lunch.api.v1.common.exception.EntityNotFoundException;
import com.yeoboya.lunch.api.v1.common.response.Code;
import com.yeoboya.lunch.api.v1.common.response.ErrorCode;
import com.yeoboya.lunch.api.v1.common.response.Response;
import com.yeoboya.lunch.api.v1.common.response.Response.Body;
import com.yeoboya.lunch.api.v1.file.domain.File;
import com.yeoboya.lunch.api.v1.file.response.FileUploadResponse;
import com.yeoboya.lunch.api.v1.file.service.FileService;
import com.yeoboya.lunch.api.v1.member.domain.Member;
import com.yeoboya.lunch.api.v1.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final HashTagRepository hashTagRepository;
    private final MemberRepository memberRepository;
    private final FileService fileService;
    private final Response response;


    public ResponseEntity<Body> saveBoard(BoardCreate boardCreate) {
        Member member = memberRepository.findByEmail(boardCreate.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("Member not found - " + boardCreate.getEmail()));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = Optional.of(authentication.getName()).orElseThrow(() -> new EntityNotFoundException(""));
        if (!boardCreate.getEmail().equals(name)) {
            return response.fail(ErrorCode.INVALID_AUTH_TOKEN);
        }

        List<BoardHashTag> boardHashTags = Optional.ofNullable(boardCreate.getHashTag())
                .orElse(Collections.emptyList())
                .stream()
                .map(tag -> hashTagRepository.existsHashTagByTag(tag)
                        ? hashTagRepository.findHashTagByTag(tag)
                        : hashTagRepository.save(HashTag.builder().tag(tag).build()))
                .map(BoardHashTag::createBoardHashTag)
                .collect(Collectors.toList());

        Board board = Board.createBoard(member, boardCreate, boardHashTags);
        try {
            Board save = boardRepository.save(board);
        } catch (DataAccessException ignored) {

        }

        return response.success(Code.SAVE_SUCCESS);
    }


    public ResponseEntity<Body> saveBoardPhoto(MultipartFile file, FileBoardCreate fileBoardCreate) {
        Member member = memberRepository.findByEmail(fileBoardCreate.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("Member not found - " + fileBoardCreate.getEmail()));

//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String name = Optional.of(authentication.getName()).orElseThrow(() -> new EntityNotFoundException(""));
//        if (!fileBoardCreate.getEmail().equals(name)) {
//            return response.fail(ErrorCode.INVALID_AUTH_TOKEN);
//        }

        List<BoardHashTag> boardHashTags = Optional.ofNullable(fileBoardCreate.getHashTag())
                .orElse(Collections.emptyList())
                .stream()
                .map(tag -> hashTagRepository.existsHashTagByTag(tag)
                        ? hashTagRepository.findHashTagByTag(tag)
                        : hashTagRepository.save(HashTag.builder().tag(tag).build()))
                .map(BoardHashTag::createBoardHashTag)
                .collect(Collectors.toList());


        FileUploadResponse upload = fileService.upload(file, fileBoardCreate.getUploadType());
        File fileBuild = File.builder().fileUploadResponse(upload).build();

        Board board = Board.createBoard(member, fileBoardCreate, boardHashTags, fileBuild);
        boardRepository.save(board);
        return response.success(Code.SAVE_SUCCESS);
    }

    public ResponseEntity<Body> list(BoardSearch boardSearch, Pageable pageable) {
        Page<Board> boards = boardRepository.boardList(boardSearch, pageable);

        List<BoardResponse> boardResponses = boards
                .stream()
                .map(BoardResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> pagination = Map.of(
                "page", boards.getNumber()+1,
                "isFirst", boards.isFirst(),
                "isLast", boards.isLast(),
                "isEmpty", boards.isEmpty(),
                "totalPages", boards.getTotalPages(),
                "totalElements", boards.getTotalElements());

        Map<String, Object> data = Map.of(
                "list", boardResponses,
                "pagination", pagination);

        return response.success(Code.SEARCH_SUCCESS, data);

    }

    public ResponseEntity<Body> findBoardById(Long boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new EntityNotFoundException("Board not found - " + boardId));;
        BoardResponse boardResponse = BoardResponse.from(board);
        return response.success(Code.SEARCH_SUCCESS, boardResponse);
    }
}
