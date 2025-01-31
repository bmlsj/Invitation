package com.coho.invitation.controller;

import com.coho.invitation.dto.Event;
import com.coho.invitation.security.UserAuthorize;
import com.coho.invitation.service.EventService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Tag(name="2) Event API",description = "행사 API")
@UserAuthorize
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {
    @Autowired
    private EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /* 로그인한 사용자의 진행중인 행사 목록 가져오기 */
    @Operation(summary = "진행중인 행사 목록 조회", description = "현재 진행 중인 행사 목록을 반환")
    @GetMapping("/progressing")
    public ResponseEntity<List<Event>> getEventsProgressing(@AuthenticationPrincipal User user){
        String uid = user.getUsername();

        List<Event> eventList = eventService.getEventsProgressing(uid);

        return ResponseEntity.ok().body(eventList);
    }

    /* 로그인한 사용자의 진행 완료된 행사 목록 가져오기 */
    @Operation(summary = "진행 완료된 행사 목록 조회", description = "현재 완료된 행사 목록을 반환")
    @GetMapping("/done")
    public ResponseEntity<List<Event>> getEventsDone(@AuthenticationPrincipal User user){
        String uid = user.getUsername();

        List<Event> eventList = eventService.getEventsDone(uid);

        return ResponseEntity.ok().body(eventList);
    }

    /* 선택한 행사 조회 */
    @Operation(summary = "특정 행사 조회", description = "파라미터로 받은 event id의 행사 정보를 반환")
    @Parameter(name="str", description = "선택한 행사의 event id를 전송")
    @GetMapping("/{eid}")
    public ResponseEntity<Event> getEvent(@PathVariable("eid") String eid, @AuthenticationPrincipal User user){
        String uid = user.getUsername();

        /* 권한이 없다면 조회 불가능 - spring interceptor로..? */
        if(!eventService.checkAuthority(eid).contains(uid))
            return null;

        //선택한 행사 조회
        Event event = eventService.getEvent(eid);

        return ResponseEntity.ok().body(event);
    }

    /* 행사 추가하기 */
    @Operation(summary = "행사 추가", description = "파라미터로 받은 행사 정보를 저장하고 event id를 반환")
    @PostMapping("")
    public ResponseEntity<Event> addEvent(@AuthenticationPrincipal User user, @RequestBody JsonNode params){
        String uid = user.getUsername();
        Event event = new Event();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

        // 실제 event 값 설정
        event.setEid(UUID.randomUUID().toString());
        event.setEtype(params.get("type").asText());
        event.setEdate(LocalDateTime.parse(params.get("datetime").asText()),formatter);
        event.setLocation(params.get("location").asText());
        event.setEhost(params.get("host").toString());

        eventService.insertEvent(event);
        eventService.insertManage(uid,event.getEid());

        return ResponseEntity.ok().body(event);
    }

    /* 행사 권한자 추가하기
    * 행사 권한자를 어떻게 호출할 것인가(백 or 프론트)*/
    @PostMapping("/auth/{eid}")
    public ResponseEntity<String> addAuthority(@AuthenticationPrincipal User user, @PathVariable("eid") String eid, @RequestBody JsonNode params){
        String uid = user.getUsername();
        String newUid = params.get("uid").asText();

        // 현재 사용자가 권한자 인지 확인
        if(!eventService.checkAuthority(eid).contains(uid))
            return ResponseEntity.ok().body("권한자 추가 실패");

        // 권한자 추가
        eventService.insertManage(newUid,eid);

        return ResponseEntity.ok().body("권한자 추가 성공");
    }

    /* 행사 정보 수정하기 */
    @Operation(summary = "행사 수정", description = "파라미터로 받은 event id와 수정할 행사 정보를 사용하여 행사 정보를 수정하고 event id 반환")
    @Parameter(name="str", description = "수정할 행사의 event id 전송")
    @PutMapping("/{eid}")
    public ResponseEntity<String> updateEvent(@AuthenticationPrincipal User user, @PathVariable("eid") String eid, @RequestBody JsonNode params){
        String uid = user.getUsername();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Event event = new Event();
        event.setEid(eid);
        event.setEtype(params.get("type").asText());
        event.setEdate(LocalDateTime.parse(params.get("datetime").asText()),formatter);
        event.setLocation(params.get("location").asText());
        event.setEhost(params.get("host").toString());

        /* 권한이 있을 경우에만 수정 가능 */
        if(!eventService.checkAuthority(eid).contains(uid))
            return ResponseEntity.ok().body("권한이 없습니다.");     // 임의로 넣어놓음

        // 행사 수정
        eventService.updateEvent(event);

        return ResponseEntity.ok().body(eid);
    }

    /* 행사 삭제하기 */
    @Operation(summary = "행사 삭제", description = "event id를 받아 행사를 삭제하고 event id 반환")
    @Parameter(name="str", description = "삭제할 행사의 event id를 전송")
    @DeleteMapping("/{eid}")
    public ResponseEntity<String> deleteEvent(@AuthenticationPrincipal User user, @PathVariable("eid") String eid){
        String uid = user.getUsername();

        /* 권한이 있을 경우에만 삭제 가능 */
        if(!eventService.checkAuthority(eid).contains(uid))
            return ResponseEntity.ok().body("권한이 없습니다.");     // 임의로 넣어놓음

        // 행사 삭제
        eventService.deleteEvent(eid);

        return ResponseEntity.ok().body(eid);
    }

    /* 행사의 권한 삭제 - 자기 자신만 가능 */
    @DeleteMapping("/auth/{eid}")
    public ResponseEntity<String> deleteAuthority(@AuthenticationPrincipal User user, @PathVariable("eid") String eid){
        String uid = user.getUsername();

        /* 현재 로그인한 사용자의 행사 권한 삭제 */
        eventService.deleteAuthority(eid,uid);

        /* 권한자가 모두 권한 해제될 경우 행사 삭제 */
        if(eventService.checkAuthority(eid).isEmpty())
            eventService.deleteEvent(eid);

        return ResponseEntity.ok().body(eid);
    }

}
