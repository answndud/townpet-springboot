package com.townpet.gathering;

import com.townpet.common.UuidV7;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.lang.Nullable;

@Service class GatheringService {
  private final GatheringRepository gatherings; private final GatheringParticipantRepository participants;
  GatheringService(GatheringRepository gatherings, GatheringParticipantRepository participants){this.gatherings=gatherings;this.participants=participants;}
  @Transactional(readOnly=true) List<GatheringView> list(){return gatherings.findByStatusOrderByStartsAtAsc(GatheringStatus.ACTIVE).stream().map(this::view).toList();}
  @Transactional(readOnly=true) GatheringView get(UUID id, @Nullable UUID viewer){return view(gatherings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), viewer);}
  @Transactional GatheringView create(UUID host, String title, String description, String location, Instant startsAt, int capacity){return view(gatherings.save(new GatheringEntity(UuidV7.randomUuid(),host,title,description,location,startsAt,capacity)));}
  @Transactional GatheringView join(UUID id, UUID member){GatheringEntity gathering=gatherings.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); if(gathering.getStatus()!=GatheringStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.CONFLICT,"Gathering is cancelled"); if(participants.findByGatheringIdAndMemberId(id,member).isPresent()) return view(gathering,member); if(participants.findAllByGatheringId(id).size()>=gathering.getCapacity()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Gathering is full"); participants.save(new GatheringParticipantEntity(UuidV7.randomUuid(),id,member)); return view(gathering,member);}
  @Transactional GatheringView leave(UUID id, UUID member){GatheringEntity gathering=gatherings.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); participants.findByGatheringIdAndMemberId(id,member).ifPresent(participants::delete); return view(gathering,member);}
  @Transactional GatheringView cancel(UUID id, UUID member){GatheringEntity gathering=gatherings.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); if(!gathering.getHostMemberId().equals(member)) throw new ResponseStatusException(HttpStatus.FORBIDDEN); gathering.cancel(); return view(gathering,member);}
  private GatheringView view(GatheringEntity g){return view(g,null);} private GatheringView view(GatheringEntity g, @Nullable UUID viewer){int count=participants.findAllByGatheringId(g.getId()).size(); boolean joined=viewer!=null&&participants.findByGatheringIdAndMemberId(g.getId(),viewer).isPresent(); return new GatheringView(g.getId(),g.getHostMemberId(),g.getTitle(),g.getDescription(),g.getLocation(),g.getStartsAt(),g.getCapacity(),count,g.getStatus(),joined,g.getVersion());}
  record GatheringView(UUID id,UUID hostMemberId,String title,String description,String location,Instant startsAt,int capacity,int participantCount,GatheringStatus status,boolean joined,long version){}
}
