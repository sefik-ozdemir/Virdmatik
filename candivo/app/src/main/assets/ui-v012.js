(function(){
"use strict";
if(window.__candivoV012UiInstalled)return;
window.__candivoV012UiInstalled=true;

const STYLE_ID="candivo-v012-ui-style";
const FIREBASE_WAIT_MS=12000;
let refreshTimer=0,lastRefresh=0;

function esc(value){return String(value==null?"":value).replace(/[&<>\"']/g,function(ch){return ({"&":"&amp;","<":"&lt;",">":"&gt;",'\"':"&quot;","'":"&#39;"})[ch];});}
function safePhoto(value){try{const u=new URL(String(value||""));return u.protocol==="https:"?u.href:"";}catch(e){return "";}}
function scoreOf(row){return Math.max(0,Number(row&&row.rankScore)||((Math.max(0,Number(row&&row.trophies)||0)*4)+(Math.max(1,Number(row&&row.level)||1)*4)+(Math.max(0,Number(row&&row.diamonds)||0)*2)));}
function avatarHtml(row){row=row||{};const photo=safePhoto(row.photoUrl);return photo?'<img src="'+esc(photo)+'" alt="" referrerpolicy="no-referrer">':'<span>'+esc(row.avatar||"🧙‍♀️")+'</span>';}
function persistentUser(){try{const u=window.firebase&&firebase.auth&&firebase.auth().currentUser;return !!(u&&!u.isAnonymous);}catch(e){return false;}}
function currentUid(){try{const u=window.firebase&&firebase.auth&&firebase.auth().currentUser;return u&&u.uid||"";}catch(e){return "";}}

function installStyle(){
  if(document.getElementById(STYLE_ID))return;
  const style=document.createElement("style");style.id=STYLE_ID;
  style.textContent=`
  .start-screen{align-items:flex-start!important;padding-top:max(10px,env(safe-area-inset-top))!important;overflow:auto!important}
  .start-screen>.card{width:min(94vw,430px)!important;max-height:none!important;margin:0 auto 20px!important;padding:14px 14px 16px!important;border:1px solid rgba(255,217,61,.32)!important;border-radius:22px!important;background:linear-gradient(180deg,rgba(28,14,47,.98),rgba(13,7,25,.985))!important;box-shadow:0 18px 60px rgba(0,0,0,.48),inset 0 1px rgba(255,255,255,.05)!important}
  .candivo-leaderboard-hero{display:grid;grid-template-columns:1fr 1fr;gap:9px;margin:0 0 10px}
  .candivo-leader-card{appearance:none!important;width:100%!important;margin:0!important;color:inherit!important;text-align:initial!important;font-family:'Nunito',sans-serif!important;position:relative;display:grid;grid-template-columns:52px minmax(0,1fr);align-items:center;gap:8px;min-height:68px;padding:8px!important;border:1px solid rgba(255,255,255,.13)!important;border-radius:16px!important;background:linear-gradient(145deg,rgba(70,35,108,.62),rgba(26,14,45,.75))!important;overflow:hidden;box-shadow:none!important}
  .candivo-leader-card.first{border-color:rgba(255,217,61,.68)!important;box-shadow:inset 0 0 20px rgba(255,217,61,.08)!important}
  .candivo-leader-card.second{border-color:rgba(184,204,231,.46)!important}
  .candivo-leader-medal{position:absolute;top:4px;right:7px;font-size:18px;filter:drop-shadow(0 2px 3px rgba(0,0,0,.55))}
  .candivo-leader-avatar{width:50px;height:50px;border-radius:50%;display:grid;place-items:center;overflow:hidden;font-size:30px;border:2px solid rgba(255,255,255,.5);background:#160d25;box-shadow:0 5px 16px rgba(0,0,0,.35)}
  .candivo-leader-card.first .candivo-leader-avatar{border-color:#ffd93d}.candivo-leader-card.second .candivo-leader-avatar{border-color:#c7d2e4}
  .candivo-leader-avatar img{width:100%;height:100%;object-fit:cover;display:block}
  .candivo-leader-info{min-width:0;text-align:left}.candivo-leader-kicker{display:block;font-size:8px;letter-spacing:.7px;font-weight:900;color:#ffd93d;text-transform:uppercase}.second .candivo-leader-kicker{color:#dbe6f4}
  .candivo-leader-name{display:block;margin-top:2px;font-size:11px;line-height:1.15;font-weight:900;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:#fff}.candivo-leader-score{display:block;margin-top:3px;font-size:9px;font-weight:800;color:rgba(255,255,255,.74)}
  .start-screen .start-logo{margin:2px auto 2px!important}.start-screen .start-logo img{width:86px!important;height:86px!important;border-radius:50%!important;filter:drop-shadow(0 7px 18px rgba(161,68,255,.35))}
  .start-screen>.card>h2{margin:0!important;font-size:25px!important;letter-spacing:.4px!important}.start-screen>.card>h2+p{margin:1px 0 8px!important;font-size:10px!important;opacity:.75}
  .candivo-my-rank{display:flex;align-items:center;justify-content:space-between;gap:9px;margin:8px 0 9px;padding:9px 10px;border:1px solid rgba(82,224,196,.28);border-radius:13px;background:rgba(82,224,196,.07);text-align:left}
  .candivo-my-rank strong{display:block;font-size:11px;color:#fff}.candivo-my-rank small{display:block;margin-top:2px;font-size:9px;color:rgba(255,255,255,.65)}.candivo-my-rank b{font-size:12px;color:#52e0c4;white-space:nowrap}
  .candivo-rank-login{appearance:none;border:1px solid rgba(255,217,61,.55)!important;background:linear-gradient(135deg,#5d2f93,#8b3fd6)!important;color:#fff!important;padding:7px 9px!important;border-radius:10px!important;font-size:9px!important;font-weight:900!important;box-shadow:none!important;margin:0!important}
  .start-screen .profile-summary{margin:7px 0!important;padding:8px!important;border-radius:13px!important;background:rgba(255,255,255,.045)!important}
  .start-screen .profile-avatar{width:38px!important;height:38px!important;font-size:23px!important}.start-screen .profile-data{font-size:9px!important;line-height:1.35!important}.start-screen .profile-data strong{font-size:12px!important}
  .start-screen .level-picker{gap:5px!important;margin:7px 0!important}.start-screen .level-picker button{min-height:34px!important;border-radius:10px!important;font-size:11px!important;font-weight:900!important}
  .start-screen .difficulty-note{margin:5px 0 8px!important;font-size:9px!important}
  .start-screen .start-actions{display:grid!important;grid-template-columns:1fr 1fr!important;gap:8px!important;margin-top:8px!important}
  .start-screen .start-actions button{min-height:44px!important;margin:0!important;padding:8px 7px!important;border-radius:13px!important;border:1px solid rgba(255,255,255,.13)!important;font:900 11px 'Nunito',sans-serif!important;letter-spacing:.05px!important;box-shadow:0 7px 16px rgba(0,0,0,.24),inset 0 1px rgba(255,255,255,.08)!important;transition:transform .12s ease,filter .12s ease!important}
  .start-screen .start-actions button:active{transform:scale(.97)!important;filter:brightness(.92)!important}
  .start-screen #startGameBtn3{grid-column:1/-1!important;min-height:49px!important;font-size:14px!important;background:linear-gradient(135deg,#ffb326,#ff6b4a)!important;color:#21110b!important;border-color:rgba(255,217,61,.8)!important}
  .start-screen #startDuelBtn3{background:linear-gradient(135deg,#6e38c6,#a34df3)!important;border-color:rgba(167,139,250,.7)!important;color:#fff!important}
  .start-screen #privateStartBtn3{background:linear-gradient(135deg,#173e66,#245c90)!important;color:#eaf6ff!important}
  .start-screen #loginStartBtn3{background:linear-gradient(135deg,#19745f,#23a482)!important;color:#f3fffb!important}
  .start-screen #socialStartBtn3,.start-screen #profileBtn3{background:linear-gradient(135deg,rgba(54,41,76,.95),rgba(37,28,55,.95))!important;color:#f5efff!important}
  @media(max-width:360px){.candivo-leader-card{grid-template-columns:43px minmax(0,1fr);padding:6px!important}.candivo-leader-avatar{width:42px;height:42px;font-size:25px}.candivo-leader-name{font-size:10px}.start-screen>.card{padding:10px!important}.start-screen .start-logo img{width:70px!important;height:70px!important}.start-screen .start-actions button{min-height:40px!important;font-size:10px!important}}
  `;
  document.head.appendChild(style);
}

function leaderSkeleton(){return '<div class="candivo-leader-card first"><span class="candivo-leader-medal">👑</span><span class="candivo-leader-avatar">…</span><span class="candivo-leader-info"><span class="candivo-leader-kicker">Dünya #1</span><span class="candivo-leader-name">Yükleniyor…</span><span class="candivo-leader-score">Zirvenin sahibi</span></span></div><div class="candivo-leader-card second"><span class="candivo-leader-medal">🥈</span><span class="candivo-leader-avatar">…</span><span class="candivo-leader-info"><span class="candivo-leader-kicker">Dünya #2</span><span class="candivo-leader-name">Yükleniyor…</span><span class="candivo-leader-score">Takipte</span></span></div>';}
function renderOwnPlaceholder(mine){
  if(persistentUser())mine.innerHTML='<span><strong>🏆 Benim Dünya Sıram</strong><small>Sıralaman hesaplanıyor…</small></span><b>…</b>';
  else mine.innerHTML='<span><strong>🌍 Dünya sıralamasındaki yerini merak ediyor musun?</strong><small>Resmî sıralamaya girmek için hesabını kalıcı yap.</small></span><button type="button" class="candivo-rank-login">Google ile giriş</button>';
  const btn=mine.querySelector("button");if(btn)btn.onclick=function(){try{if(window.CandivoSocial&&CandivoSocial.showAccount)CandivoSocial.showAccount();}catch(e){}};
}
function ensureSlots(){
  const card=document.querySelector(".start-screen>.card");if(!card)return null;
  const logo=card.querySelector(".start-logo");if(logo){const img=logo.querySelector("img");if(img){img.src="https://www.73haber.com.tr/candivo/game-logo.webp";img.alt="Candivo";}}
  let hero=card.querySelector("#candivoLeaderHero");
  if(!hero){hero=document.createElement("div");hero.id="candivoLeaderHero";hero.className="candivo-leaderboard-hero";hero.innerHTML=leaderSkeleton();card.insertBefore(hero,logo||card.firstChild);}
  let mine=card.querySelector("#candivoMyRank");
  if(!mine){mine=document.createElement("div");mine.id="candivoMyRank";mine.className="candivo-my-rank";const profile=card.querySelector(".profile-summary");if(profile)profile.insertAdjacentElement("beforebegin",mine);else (logo||hero).insertAdjacentElement("afterend",mine);}
  renderOwnPlaceholder(mine);
  return {card,hero,mine};
}
function renderLeaders(hero,rows){
  if(!rows||!rows.length){hero.innerHTML='<div class="candivo-leader-card first" style="grid-column:1/-1"><span class="candivo-leader-avatar">🌍</span><span class="candivo-leader-info"><span class="candivo-leader-kicker">Dünya Sıralaması</span><span class="candivo-leader-name">İlk şampiyon aranıyor</span><span class="candivo-leader-score">Zirveye çıkan ilk oyuncu sen olabilirsin.</span></span></div>';return;}
  hero.innerHTML=rows.slice(0,2).map(function(row,i){const first=i===0,s=scoreOf(row);return '<div class="candivo-leader-card '+(first?'first':'second')+'"><span class="candivo-leader-medal">'+(first?'👑':'🥈')+'</span><span class="candivo-leader-avatar">'+avatarHtml(row)+'</span><span class="candivo-leader-info"><span class="candivo-leader-kicker">Dünya #'+(i+1)+'</span><span class="candivo-leader-name">'+esc(row.username||'Oyuncu')+'</span><span class="candivo-leader-score">🏆 '+Math.max(0,Number(row.trophies)||0)+' · 📊 '+(s/10).toFixed(s%10?1:0)+'</span></span></div>';}).join('');
}
async function waitFirebase(){const start=Date.now();while(Date.now()-start<FIREBASE_WAIT_MS){try{if(window.firebase&&firebase.apps&&firebase.apps.length&&firebase.firestore&&firebase.auth&&firebase.auth().currentUser)return true;}catch(e){}await new Promise(r=>setTimeout(r,250));}return false;}
async function loadRanking(){
  const slots=ensureSlots();if(!slots)return;
  if(!(await waitFirebase())){slots.hero.innerHTML=leaderSkeleton();return;}
  try{
    const db=firebase.firestore(),snap=await db.collection("users").limit(5000).get(),rows=[];snap.forEach(function(doc){const d=doc.data()||{};rows.push(Object.assign({uid:doc.id},d));});rows.sort(function(a,b){return scoreOf(b)-scoreOf(a)||(Number(b.trophies)||0)-(Number(a.trophies)||0)||String(a.username||'').localeCompare(String(b.username||''),'tr');});
    renderLeaders(slots.hero,rows);
    if(persistentUser()){
      const uid=currentUid(),idx=rows.findIndex(function(r){return r.uid===uid;}),rank=idx>=0?idx+1:0,me=idx>=0?rows[idx]:null;
      slots.mine.innerHTML='<span><strong>🏆 Benim Dünya Sıram</strong><small>'+(rank?'Zirveye '+Math.max(0,rank-1)+' oyuncu kaldı · 🏆 '+Math.max(0,Number(me&&me.trophies)||0):'Profilin sıralamaya hazırlanıyor…')+'</small></span><b>'+(rank?'#'+rank:'—')+'</b>';
      slots.mine.onclick=function(){try{if(window.CandivoSocial&&CandivoSocial.showSocial)CandivoSocial.showSocial();}catch(e){}};
      slots.mine.style.cursor='pointer';
    }else renderOwnPlaceholder(slots.mine);
  }catch(e){
    console.warn('Candivo v0.12 ranking hero:',e);
    slots.hero.innerHTML='<div class="candivo-leader-card first" style="grid-column:1/-1"><span class="candivo-leader-avatar">🌍</span><span class="candivo-leader-info"><span class="candivo-leader-kicker">Dünya Liderleri</span><span class="candivo-leader-name">Sıralama yenileniyor…</span><span class="candivo-leader-score">Bağlantı kurulunca otomatik görünecek.</span></span></div>';
  }
}
function enhance(){installStyle();const slots=ensureSlots();if(!slots)return;const now=Date.now();if(now-lastRefresh>3000){lastRefresh=now;clearTimeout(refreshTimer);refreshTimer=setTimeout(loadRanking,150);}}
const observer=new MutationObserver(function(){enhance();});
observer.observe(document.documentElement,{childList:true,subtree:true});
setInterval(enhance,2500);
enhance();
})();
