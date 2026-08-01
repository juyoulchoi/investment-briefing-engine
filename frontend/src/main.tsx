import React,{useEffect,useState}from"react";import{createRoot}from"react-dom/client";import"./styles.css";
type Page="dashboard"|"briefing"|"holdings"|"regular"|"additional"|"rebalance"|"history";
const nav:[Page,string,string][]=[["dashboard","대시보드","⌂"],["briefing","투자 브리핑","▤"],["holdings","보유종목","◇"],["regular","정기매수","↻"],["additional","추가매수","+"],["rebalance","리밸런싱","⇄"],["history","브리핑 이력","◷"]];
const pageStorageKey="investment-briefing-page";
const savedPage=()=>{const value=localStorage.getItem(pageStorageKey);return nav.some(([page])=>page===value)?value as Page:"dashboard"};
type Account={accountId:number;accountType:"DOMESTIC"|"OVERSEAS"|"ISA"|"PENSION";cashAmount:number|null;reservedCashAmount:number|null};
type Holding={holdingId:number;accountId:number;accountName:string;accountType:Account["accountType"];stockId:number;ticker:string;stockName:string;holdingQuantity:number;averagePrice:number;currentPrice:number;evaluationAmount:number;profitLossRate:number;targetWeight:number|null;currentWeight:number|null;weightStatus:string|null;holdingStatus:string};
type PageData<T>={content:T[]};type ApiResult<T>={success:boolean;data:T;error?:{message?:string}};
const accountOrder:Record<Account["accountType"],number>={DOMESTIC:0,OVERSEAS:1,ISA:2,PENSION:3};
const accountLabel:Record<Account["accountType"],string>={DOMESTIC:"국내주식",OVERSEAS:"해외주식",ISA:"ISA",PENSION:"연금"};
const accountTypes:Account["accountType"][]=["DOMESTIC","OVERSEAS","ISA","PENSION"];
const accountTabStorageKey="investment-briefing-account-tab";
const savedAccountTab=()=>{const value=localStorage.getItem(accountTabStorageKey);return accountTypes.includes(value as Account["accountType"])?value as Account["accountType"]:"DOMESTIC"};
const overseasName:Record<string,string>={BOTZ:"BOTZ",HYDR:"HYDR",QQQ:"QQQ",SCHD:"SCHD",SMH:"SMH",SPY:"SPY",VIG:"VIG",XLF:"XLF",XLI:"XLI",XLV:"XLV",GEV:"GE 버노바",MSFT:"마이크로소프트",BAC:"뱅크오브아메리카","BRK.B":"버크셔 해서웨이 B",VRT:"버티브 홀딩스",AVGO:"브로드컴",VST:"비스트라 에너지",V:"비자",SPCX:"스페이스X",ANET:"아리스타 네트웍스",AMZN:"아마존닷컴",IONQ:"아이온큐",GOOGL:"알파벳A",ABBV:"애브비",AAPL:"애플",NVDA:"엔비디아",WMT:"월마트",INTC:"인텔",LLY:"일라이 릴리",JPM:"제이피모간체이스",JNJ:"존슨앤드존슨",CAT:"캐터필러",COST:"코스트코 홀세일",CEG:"콘스텔레이션 에너지",PLTR:"팔란티어",PLUG:"플러그파워"};
async function api<T>(url:string):Promise<T>{const response=await fetch(url);if(!response.ok)throw new Error(`HTTP ${response.status}`);const body=await response.json() as ApiResult<T>;if(!body.success)throw new Error(body.error?.message||"데이터 조회에 실패했습니다.");return body.data;}
const won=(n:number)=>`${n.toLocaleString("ko-KR")}원`;
const amount=(n:number,overseas=false)=>n.toLocaleString(overseas?"en-US":"ko-KR",{minimumFractionDigits:overseas?2:0,maximumFractionDigits:overseas?2:0});
function App(){const[page,setPage]=useState<Page>(savedPage),[brief,setBrief]=useState("오늘"),[history,setHistory]=useState("일일"),[reb,setReb]=useState("주간"),[toast,setToast]=useState("");const move=(p:Page)=>{localStorage.setItem(pageStorageKey,p);setPage(p);scrollTo({top:0,behavior:"smooth"})},notify=(m:string)=>{setToast(m);setTimeout(()=>setToast(""),2200)};return <div className="shell"><aside><button className="brand" onClick={()=>move("dashboard")}><b>F</b><span><strong>FINBRIEF</strong><small>Investment OS</small></span></button><nav><p>OVERVIEW</p>{nav.slice(0,1).map(x=><Nav key={x[0]} x={x} active={page===x[0]} go={move}/>)}<p>INVEST</p>{nav.slice(1,6).map(x=><Nav key={x[0]} x={x} active={page===x[0]} go={move}/>)}<p>RECORDS</p>{nav.slice(6).map(x=><Nav key={x[0]} x={x} active={page===x[0]} go={move}/>)}</nav><div className="user"><b>민</b><span><strong>김투자</strong><small>개인 포트폴리오</small></span></div></aside><main><header className="top"><div><h1>{nav.find(x=>x[0]===page)?.[1]}</h1><p>2026년 7월 30일 목요일 · 장 마감 기준</p></div><div><span className="latest">● 데이터 최신</span><button className="primary" onClick={()=>notify("최신 시장 데이터로 업데이트했습니다.")}>↻ 브리핑 갱신</button></div></header>{page==="dashboard"&&<Dashboard go={move}/>} {page==="briefing"&&<Briefing period={brief} set={setBrief}/>} {page==="holdings"&&<Holdings/>} {page==="regular"&&<Regular notify={notify}/>} {page==="additional"&&<Additional notify={notify}/>} {page==="rebalance"&&<Rebalance period={reb} set={setReb} notify={notify}/>} {page==="history"&&<History period={history} set={setHistory}/>}</main><nav className="mobile">{nav.slice(0,5).map(x=><button key={x[0]} className={page===x[0]?"active":""} onClick={()=>move(x[0])}><b>{x[2]}</b>{x[1]}</button>)}</nav>{toast&&<div className="toast">{toast}</div>}</div>}
function Nav({x,active,go}:{x:[Page,string,string],active:boolean,go:(p:Page)=>void}){return <button className={`nav ${active?"active":""}`} onClick={()=>go(x[0])}><b>{x[2]}</b>{x[1]}{x[0]==="briefing"&&<i>3</i>}</button>}
function Dashboard({ go }: { go: (p: Page) => void }) {
  type AssetAccount = {
    type: Account["accountType"];
    value: number;
    evaluation: number;
    cost: number;
    holdingCount: number;
  };
  const [assetAccounts, setAssetAccounts] = useState<AssetAccount[]>([]),
    [assetLoading, setAssetLoading] = useState(true),
    [assetError, setAssetError] = useState("");

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const accountPage = await api<PageData<Account>>(
          "/api/v1/accounts?size=100",
        );
        const accounts = accountPage.content;
        const pages = await Promise.all(
          accounts.map((account) =>
            api<PageData<Holding>>(
              `/api/v1/holdings?accountId=${account.accountId}&size=100`,
            ),
          ),
        );
        const grouped = new Map<
          Account["accountType"],
          Omit<AssetAccount, "type">
        >();
        accounts.forEach((account, index) => {
          const holdings = pages[index].content;
          const current = grouped.get(account.accountType) || {
            value: 0,
            evaluation: 0,
            cost: 0,
            holdingCount: 0,
          };
          const evaluation = holdings.reduce(
            (sum, holding) => sum + Number(holding.evaluationAmount || 0),
            0,
          );
          const cost = holdings.reduce((sum, holding) => {
            const value = Number(holding.evaluationAmount || 0);
            const rate = Number(holding.profitLossRate || 0);
            return sum + (rate <= -100 ? value : value / (1 + rate / 100));
          }, 0);
          grouped.set(account.accountType, {
            value:
              current.value +
              evaluation +
              Number(account.cashAmount || 0) +
              Number(account.reservedCashAmount || 0),
            evaluation: current.evaluation + evaluation,
            cost: current.cost + cost,
            holdingCount: current.holdingCount + holdings.length,
          });
        });
        if (alive)
          setAssetAccounts(
            accountTypes.map((type) => ({
              type,
              ...(grouped.get(type) || {
                value: 0,
                evaluation: 0,
                cost: 0,
                holdingCount: 0,
              }),
            })),
          );
      } catch (error) {
        if (alive)
          setAssetError(
            error instanceof Error
              ? error.message
              : "계좌 현황을 불러오지 못했습니다.",
          );
      } finally {
        if (alive) setAssetLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, []);

  const total = assetAccounts.reduce((sum, account) => sum + account.value, 0),
    totalEvaluation = assetAccounts.reduce(
      (sum, account) => sum + account.evaluation,
      0,
    ),
    totalCost = assetAccounts.reduce((sum, account) => sum + account.cost, 0),
    totalRate =
      totalCost > 0 ? ((totalEvaluation - totalCost) / totalCost) * 100 : 0;
  const rateText = (account: AssetAccount) =>
    account.holdingCount === 0
      ? "대기"
      : `${account.evaluation - account.cost >= 0 ? "+" : ""}${
          account.cost > 0
            ? (
                ((account.evaluation - account.cost) / account.cost) *
                100
              ).toFixed(1)
            : "0.0"
        }%`;

  return (
    <div className="page">
      <section className="hero">
        <div>
          <K>Today's market</K>
          <h2>
            시장은 <em>중립</em>, 지금은 선별적으로 움직일 때입니다.
          </h2>
          <p>
            과열 부담은 낮아졌지만 추세 확신은 아직 부족합니다. 정기매수는
            유지하고 추가매수는 현금의 20% 이내로 제한하세요.
          </p>
        </div>
        <div className="ring">
          <strong>68</strong>
          <small>시장점수</small>
        </div>
      </section>
      <section className="metrics">
        <Metric t="시장심리" v="중립" d="전일 대비 +4" i="◒" />
        <Metric t="시장국면" v="회복 초기" d="약세 → 회복 전환" i="↗" />
        <Metric t="행동신호" v="선별 매수" d="4개 종목 중 2개" i="⚑" />
        <Metric t="현금비중" v="18.6%" d="목표 20% · 1.4%p 부족" i="₩" />
      </section>
      <div className="grid">
        <section className="card">
          <Head
            k="MY ASSETS"
            t="계좌 현황"
            a="전체보기"
            click={() => go("holdings")}
          />
          {assetLoading ? (
            <div className="data-state">계좌 현황을 불러오는 중입니다.</div>
          ) : assetError ? (
            <div className="data-state error">{assetError}</div>
          ) : (
            <>
              <div className="total">
                <span>
                  총 자산<strong>₩ {total.toLocaleString("ko-KR")}</strong>
                </span>
                <b className={totalRate >= 0 ? "pos" : "neg"}>
                  {totalRate >= 0 ? "+" : ""}
                  {totalRate.toFixed(1)}%<small>총 수익률</small>
                </b>
              </div>
              <div className="alloc">
                {assetAccounts.map((account) => (
                  <i
                    key={account.type}
                    style={{
                      width: `${total > 0 ? (account.value / total) * 100 : 25}%`,
                    }}
                  />
                ))}
              </div>
              <div className="accounts">
                {assetAccounts.map((account) => (
                  <Account
                    key={account.type}
                    n={accountLabel[account.type]}
                    v={won(account.value)}
                    p={`${(total > 0 ? (account.value / total) * 100 : 0).toFixed(1)}%`}
                    g={rateText(account)}
                  />
                ))}
              </div>
            </>
          )}
        </section>
        <section className="card">
          <Head
            k="ACTION SIGNAL"
            t="오늘의 행동신호"
            a="상세보기"
            click={() => go("additional")}
          />
          <Signal
            tag="BUY"
            n="QQQ 비중 확대"
            d="목표비중 대비 2.9%p 부족"
            v="320,000원"
          />
          <Signal
            tag="●"
            n="Microsoft 정기매수"
            d="기존 계획대로 실행"
            v="180,000원"
          />
          <Signal tag="●" n="삼성전자 관망" d="수급 반전 확인 필요" v="0원" />
          <button className="full" onClick={() => go("regular")}>
            오늘 추천매수 총 500,000원 <span>계획 확인 →</span>
          </button>
        </section>
      </div>
      <section className="card briefing">
        <Head
          k="DAILY BRIEFING"
          t="오늘 브리핑"
          a="전체 브리핑 읽기"
          click={() => go("briefing")}
        />
        <div className="columns">
          <Article
            no="01"
            t="금리 불확실성 완화, 성장주에 우호적 환경"
            d="미국 10년물 금리가 안정 구간에 진입하며 기술주 부담이 낮아졌습니다."
          />
          <Article
            no="02"
            t="원화 강세 전환 가능성, 환율 분할 대응"
            d="환율이 1,360원 아래로 내려갈 경우 환전 비중을 늘리세요."
          />
          <Article
            no="03"
            t="반도체 실적 시즌, 숫자로 확인할 때"
            d="기대감보다 실제 가이던스에 따라 종목별 차별화가 커질 전망입니다."
          />
        </div>
      </section>
    </div>
  );
}

const K=({children}:{children:React.ReactNode})=><span className="kicker">{children}</span>;const Head=({k,t,a,click}:{k:string,t:string,a:string,click?:()=>void})=><header className="head"><div><K>{k}</K><h3>{t}</h3></div><button onClick={click}>{a} →</button></header>;
function Metric({t,v,d,i}:{t:string,v:string,d:string,i:string}){return <article className="metric"><b>{i}</b><span><small>{t}</small><strong>{v}</strong><em>{d}</em></span></article>};function Account({n,v,p,g}:{n:string,v:string,p:string,g:string}){return <div><i/><b>{n}</b><span>{v}</span><small>{p}</small><em className={g[0]==="+"?"pos":g[0]==="-"?"neg":""}>{g}</em></div>};function Signal({tag,n,d,v}:{tag:string,n:string,d:string,v:string}){return <div className="signal"><b>{tag}</b><span><strong>{n}</strong><small>{d}</small></span><em>{v}</em></div>};function Article({no,t,d}:{no:string,t:string,d:string}){return <article><b>{no}</b><span><strong>{t}</strong><p>{d}</p><small>시장 인사이트</small></span></article>}
function Tabs({vals,active,set}:{vals:string[],active:string,set:(s:string)=>void}){return <div className="tabs">{vals.map(x=><button key={x} className={x===active?"active":""} onClick={()=>set(x)}>{x}</button>)}</div>}
function Briefing({period,set}:{period:string,set:(s:string)=>void}){const data:{[k:string]:[string,string,string[]]}={오늘:["선별적 위험선호가 돌아오고 있습니다","변동성은 잦아들었지만 실적 가시성이 높은 대형 성장주 중심의 접근이 유효합니다.",["정기매수 계획 유지","QQQ 목표비중까지 분할 매수","현금비중 18% 이상 방어"]],주간:["실적이 금리보다 중요한 한 주","빅테크 실적 발표와 가이던스가 지수 방향을 결정할 가능성이 높습니다.",["주간 매수금액 50만원 유지","실적 발표 후 변동성 활용","달러 환전 3회 분할"]],월간:["방어에서 균형으로 포트폴리오 전환","현금 비중을 일부 줄이고 우량 성장주 목표비중을 점진적으로 복원할 구간입니다.",["QQQ 목표비중 22% → 24%","현금 목표비중 22% → 20%","국내주식 비중 유지"]]};let d=data[period];return <div className="page"><Tabs vals={["오늘","주간","월간"]} active={period} set={set}/><section className="briefHero"><K>2026.07.30 · INVESTMENT BRIEF</K><h2>{d[0]}</h2><p>{d[1]}</p><div>{d[2].map((x,i)=><article key={x}><b>0{i+1}</b>{x}</article>)}</div></section><div className="grid briefgrid"><section className="card prose"><K>MARKET VIEW</K><h3>지금 시장을 보는 관점</h3><p>시장점수는 68점으로 중립 상단입니다. 공포 구간은 지났지만 상승 추세가 완전히 굳어졌다고 보기는 어렵습니다. 지수 전체를 쫓기보다 이익 전망이 유지되는 종목에 매수 자금을 배분하는 편이 좋습니다.</p><p>이번 구간의 핵심은 <mark>계획한 위험 안에서 시장에 머무르는 것</mark>입니다.</p></section><section className="card checks"><K>THIS PERIOD</K><h3>{period} 체크리스트</h3>{d[2].map((x,i)=><label key={x}><input type="checkbox" defaultChecked={i===0}/>{x}</label>)}</section></div></div>}
function Holdings(){
 type Draft={holdingQuantity:string;averagePrice:string};
 const[holdings,setHoldings]=useState<Holding[]>([]),[selected,setSelected]=useState<Account["accountType"]>(savedAccountTab),[sort,setSort]=useState("profitAsc"),[loading,setLoading]=useState(true),[error,setError]=useState(""),[editMode,setEditMode]=useState(false),[drafts,setDrafts]=useState<Record<number,Draft>>({}),[saving,setSaving]=useState(false),[saveError,setSaveError]=useState("");
 useEffect(()=>{let alive=true;(async()=>{try{const accountPage=await api<PageData<Account>>("/api/v1/accounts?size=100");const accounts=[...accountPage.content].sort((a,b)=>accountOrder[a.accountType]-accountOrder[b.accountType]);const pages=await Promise.all(accounts.map(a=>api<PageData<Holding>>(`/api/v1/holdings?accountId=${a.accountId}&size=100`)));if(alive)setHoldings(pages.flatMap((p,i)=>p.content.map(h=>({...h,accountName:accountLabel[accounts[i].accountType],accountType:accounts[i].accountType}))));}catch(e){if(alive)setError(e instanceof Error?e.message:"보유종목을 불러오지 못했습니다.");}finally{if(alive)setLoading(false);}})();return()=>{alive=false};},[]);
 const isOverseas=selected==="OVERSEAS",isWholeWonAccount=selected==="ISA"||selected==="PENSION",holdingEvaluation=(h:Holding)=>isOverseas?Number(h.currentPrice||0)*Number(h.holdingQuantity||0):Number(h.evaluationAmount||0),formatMoney=(n:number)=>amount(n,isOverseas);
 const rows=holdings.filter(h=>h.accountType===selected).sort((a,b)=>sort==="profitAsc"?Number(a.profitLossRate)-Number(b.profitLossRate):sort==="profitDesc"?Number(b.profitLossRate)-Number(a.profitLossRate):holdingEvaluation(b)-holdingEvaluation(a)),evaluation=rows.reduce((sum,h)=>sum+holdingEvaluation(h),0),cost=rows.reduce((sum,h)=>sum+Number(h.averagePrice||0)*Number(h.holdingQuantity||0),0),profit=evaluation-cost;
 const beginEdit=()=>{setDrafts(Object.fromEntries(rows.map(h=>[h.holdingId,{holdingQuantity:String(h.holdingQuantity),averagePrice:String(h.averagePrice)}])));setSaveError("");setEditMode(true);};
 const cancelEdit=()=>{if(!saving){setEditMode(false);setDrafts({});setSaveError("");}};
 const selectAccount=(type:Account["accountType"])=>{cancelEdit();localStorage.setItem(accountTabStorageKey,type);setSelected(type);};
 const updateDraft=(id:number,field:keyof Draft,value:string)=>setDrafts(current=>({...current,[id]:{...current[id],[field]:value}}));
 const changed=rows.filter(h=>drafts[h.holdingId]&&(Number(drafts[h.holdingId].holdingQuantity)!==Number(h.holdingQuantity)||Number(drafts[h.holdingId].averagePrice)!==Number(h.averagePrice)));
 const saveAccount=async()=>{if(!changed.length){cancelEdit();return;}for(const h of changed){const d=drafts[h.holdingId];if(!Number.isFinite(Number(d.holdingQuantity))||Number(d.holdingQuantity)<0||!Number.isFinite(Number(d.averagePrice))||Number(d.averagePrice)<0){setSaveError("보유수량과 평단가는 0 이상의 숫자로 입력하세요.");return;}}setSaving(true);setSaveError("");try{const accountId=rows[0]?.accountId;if(!accountId)throw new Error("계좌를 찾을 수 없습니다.");const response=await fetch(`/api/v1/accounts/${accountId}/holdings`,{method:"PATCH",headers:{"Content-Type":"application/json"},body:JSON.stringify({updates:changed.map(h=>({holdingId:h.holdingId,values:{holdingQuantity:Number(drafts[h.holdingId].holdingQuantity),averagePrice:Number(drafts[h.holdingId].averagePrice)}}))})});const body=await response.json() as ApiResult<Holding[]>;if(!response.ok||!body.success)throw new Error(body.error?.message||`HTTP ${response.status}`);const updated=new Map(body.data.map(h=>[h.holdingId,h]));setHoldings(current=>current.map(h=>updated.has(h.holdingId)?{...h,...updated.get(h.holdingId),accountType:h.accountType,accountName:h.accountName}:h));setEditMode(false);setDrafts({});}catch(err){setSaveError(err instanceof Error?err.message:"일괄 저장하지 못했습니다.");}finally{setSaving(false);}};
 return <div className="page"><div className="holding-toolbar"><div className="tabs account-tabs">{accountTypes.map(type=><button key={type} className={selected===type?"active":""} onClick={()=>selectAccount(type)} disabled={saving}>{accountLabel[type]}</button>)}</div><div className="holding-actions"><label className="sort-select"><span>정렬</span><select value={sort} onChange={e=>setSort(e.target.value)} disabled={editMode}><option value="profitAsc">수익률 낮은순</option><option value="profitDesc">수익률 높은순</option><option value="evaluationDesc">평가금액 많은순</option></select></label>{editMode?<><button className="cancel-button" onClick={cancelEdit} disabled={saving}>취소</button><button className="primary" onClick={saveAccount} disabled={saving}>{saving?"저장 중...":`변경 ${changed.length}건 저장`}</button></>:<button className="primary" onClick={beginEdit} disabled={loading||rows.length===0}>수정</button>}</div></div>{saveError&&<div className="batch-error">{saveError}</div>}<Summary evaluation={evaluation} profit={profit} format={formatMoney}/><section className="card table holdings-table"><Head k="PORTFOLIO" t={`${accountLabel[selected]} 보유종목`} a={loading?"조회 중":`단위: ${isOverseas?"USD":"원"}`}/>{error&&<div className="data-state error">{error}</div>}<Table heads={["종목","티커","보유수량","평균단가","현재가","평가금액","손익률","목표비중","현재비중","상태"]}>{loading?<tr><td colSpan={10} className="data-state">보유종목을 불러오는 중입니다.</td></tr>:rows.length===0&&!error?<tr><td colSpan={10} className="data-state">등록된 보유종목이 없습니다.</td></tr>:rows.map(h=><tr key={h.holdingId} className={editMode&&changed.some(x=>x.holdingId===h.holdingId)?"changed-row":""}><td><strong>{selected==="OVERSEAS"?(overseasName[h.ticker]||h.stockName):h.stockName}</strong></td><td>{h.ticker}</td><td>{editMode?<input className="inline-edit" type="number" min="0" step="0.00000001" value={drafts[h.holdingId]?.holdingQuantity??""} onChange={e=>updateDraft(h.holdingId,"holdingQuantity",e.target.value)}/>:Number(h.holdingQuantity).toLocaleString("ko-KR")}</td><td>{editMode?<input className="inline-edit price" type="number" min="0" step={isWholeWonAccount?"1":"0.000001"} value={drafts[h.holdingId]?.averagePrice??""} onChange={e=>updateDraft(h.holdingId,"averagePrice",e.target.value)}/>:formatMoney(Number(h.averagePrice))}</td><td>{formatMoney(Number(h.currentPrice))}</td><td><strong>{formatMoney(holdingEvaluation(h))}</strong></td><td className={Number(h.profitLossRate)>=0?"pos":"neg"}>{Number(h.profitLossRate)>=0?"+":""}{Number(h.profitLossRate).toFixed(2)}%</td><td>{h.targetWeight==null?"-":`${Number(h.targetWeight).toFixed(2)}%`}</td><td>{h.currentWeight==null?"-":`${Number(h.currentWeight).toFixed(2)}%`}</td><td><Badge buy={h.holdingStatus==="ACTIVE"}>{h.holdingStatus==="ACTIVE"?"보유":"비활성"}</Badge></td></tr>)}</Table></section></div>;
}const Summary=({evaluation,profit,format=won}:{evaluation:number;profit:number;format?:(n:number)=>string})=> <section className="summary" style={{gridTemplateColumns:"repeat(2,1fr)"}}><div><small>평가금액</small><strong>{format(evaluation)}</strong></div><div><small>평가손익</small><strong className={profit>=0?"pos":"neg"}>{profit>=0?"+":""}{format(profit)}</strong></div></section>;const Badge=({children,buy}:{children:React.ReactNode,buy?:boolean})=><span className={`badge ${buy?"buy":""}`}>{children}</span>;const Table=({heads,children}:{heads:string[],children:React.ReactNode})=><div className="tablewrap"><table><thead><tr>{heads.map(x=><th key={x}>{x}</th>)}</tr></thead><tbody>{children}</tbody></table></div>;
function Regular({notify}:{notify:(s:string)=>void}){let rows=[["Microsoft",90000,"2.0×",180000],["QQQ",160000,"2.0×",320000],["Apple",100000,"0×",0]] as [string,number,string,number][];return <div className="page"><section className="purchase"><div><K>REGULAR BUY PLAN</K><h2>시장 신호에 따라 매수 강도를 조절합니다.</h2><p>최소금액은 유지하되, 현재 시장점수 68점을 반영했습니다.</p></div><div><small>이번 회차 실제금액</small><strong>500,000원</strong><span>최소금액 대비 +42.9%</span></div></section><section className="card table"><Table heads={["종목","최소금액","시장 배수","실제금액","상태"]}>{rows.map(r=><tr key={r[0]}><td><strong>{r[0]}</strong><small>매월 30일</small></td><td>{won(r[1])}</td><td>{r[2]}</td><td><strong>{won(r[3])}</strong></td><td><Badge buy={!!r[3]}>{r[3]?"매수 예정":"이번 회차 보류"}</Badge></td></tr>)}</Table><footer><span>예상 주문일 2026.07.31</span><button className="primary" onClick={()=>notify("정기매수 계획을 확정했습니다.")}>매수 계획 확정</button></footer></section></div>}
function Additional({notify}:{notify:(s:string)=>void}){let rs=[["QQQ","목표비중 미달 · 회복 초기 수혜",800000,86],["Microsoft","실적 신뢰도 상위 · 밸류 부담 완화",500000,78],["Apple","비중 충분 · 조정 시 접근",300000,64]] as [string,string,number,number][];return <div className="page"><section className="cash"><div><small>추가매수 확보현금</small><strong>8,000,000원</strong><p>전체 현금 중 33.5% · 최대 사용 권장액 1,600,000원</p></div><div><i><b/></i><span>오늘 사용 권장 한도 <strong>20%</strong></span></div></section><section className="card recommends"><Head k="OPPORTUNITY" t="추천종목" a="우선순위 순"/>{rs.map((r,i)=><article key={r[0]}><b>0{i+1}</b><span><strong>{r[0]}</strong><small>{r[1]}</small></span><em>추천확신 {r[3]}</em><strong>{won(r[2])}</strong><button onClick={()=>notify(`${r[0]} 매수안을 선택했습니다.`)}>선택</button></article>)}</section></div>}
function Rebalance({period,set,notify}:{period:string,set:(s:string)=>void,notify:(s:string)=>void}){let rows=period==="주간"?[["QQQ","160,000원","320,000원","+160,000원"],["Microsoft","90,000원","180,000원","+90,000원"],["Apple","100,000원","0원","-100,000원"]]:[["QQQ","22%","24%","+2%p"],["현금","22%","20%","-2%p"],["Apple","20%","20%","유지"]];return <div className="page"><Tabs vals={["주간","월간"]} active={period} set={set}/><section className="card rebalance"><Head k={period==="주간"?"AMOUNT REBALANCE":"WEIGHT REBALANCE"} t={period==="주간"?"이번 주 금액 변경":"이번 달 목표비중 변경"} a="3개 조정"/><p>시장 국면 변화에 맞춰 {period==="주간"?"이번 주 매수금액":"장기 목표비중"}을 조정합니다.</p>{rows.map(r=><article key={r[0]}><strong>{r[0]}</strong><span>{r[1]}</span><b>→</b><span>{r[2]}</span><em className={r[3][0]==="+"?"pos":r[3][0]==="-"?"neg":""}>{r[3]}</em></article>)}<button className="primary apply" onClick={()=>notify(`${period} 리밸런싱 변경안을 저장했습니다.`)}>변경안 적용</button></section></div>}
function History({period,set}:{period:string,set:(s:string)=>void}){let rs=[["2026.07.30","선별적 위험선호가 돌아오고 있습니다",68,"회복 초기"],["2026.07.29","FOMC 대기, 계획 밖 매수는 잠시 멈춤",61,"중립"],["2026.07.28","기술주 조정은 아직 정상 범위입니다",64,"중립"],["2026.07.25","실적 시즌 전 현금 여력을 확인하세요",58,"경계"]];return <div className="page"><Tabs vals={["일일","주간","월간"]} active={period} set={set}/><section className="card history"><Head k="BRIEFING ARCHIVE" t={`${period} 브리핑 이력`} a="4건"/>{rs.map((r,i)=><article key={r[0] as string}><time>{r[0]}</time><span><strong>{r[1]}</strong><small>{period} 시장 판단과 포트폴리오 행동 가이드</small></span><em>시장점수 <b>{r[2]}</b></em><Badge buy={!i}>{r[3]}</Badge><button>→</button></article>)}</section></div>}
createRoot(document.getElementById("root")!).render(<App/>);
